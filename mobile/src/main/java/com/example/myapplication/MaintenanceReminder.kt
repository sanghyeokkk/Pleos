package com.example.myapplication

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.capability.AccessType
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object MaintenanceReminders {
    private const val CHANNEL = "maintenance_due"
    private const val JOB_ID = 724101
    private const val NOTIFICATION_ID = 724102
    private const val PREFS = "maintenance_reminders"
    private const val ENABLED = "enabled"

    fun enabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(ENABLED, false)

    fun restoreIfNeeded(context: Context): Boolean {
        if (!enabled(context)) return true
        val scheduler = context.getSystemService(JobScheduler::class.java)
        return scheduler.getPendingJob(JOB_ID) != null || setEnabled(context, true)
    }

    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        val scheduler = context.getSystemService(JobScheduler::class.java)
        if (enabled) {
            val job = JobInfo.Builder(JOB_ID, ComponentName(context, MaintenanceReminderJob::class.java))
                .setPeriodic(24 * 60 * 60 * 1000L)
                .setPersisted(true)
                .build()
            if (scheduler.schedule(job) != JobScheduler.RESULT_SUCCESS) return false
        } else {
            scheduler.cancel(JOB_ID)
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(ENABLED, enabled).commit()
    }

    fun notifyDue(context: Context, currentKm: Double? = null) {
        if (!enabled(context) || !NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val records = try { MaintenanceStore(context).load() } catch (_: Exception) { return }
        val count = dueCount(records, currentKm)
        if (count == 0) {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
            return
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "정비 점검 알림", NotificationManager.IMPORTANCE_DEFAULT))
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("정비 점검 시기가 되었어요")
            .setContentText(if (currentKm == null) "날짜 기준으로 점검할 항목이 ${count}개 있습니다. 차량 거리는 확인하지 못했습니다." else "날짜 또는 차량 주행거리 기준으로 점검할 항목이 ${count}개 있습니다.")
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        try { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) } catch (_: SecurityException) { }
    }
}

class MaintenanceReminderJob : JobService() {
    @Volatile private var worker: Thread? = null
    override fun onStartJob(params: JobParameters?): Boolean {
        if (params == null) return false
        val task = Thread {
            val latch = java.util.concurrent.CountDownLatch(1)
            val value = java.util.concurrent.atomic.AtomicReference<Double?>(null)
            var vehicle: Vehicle? = null
            try {
                vehicle = Vehicle(applicationContext)
                vehicle.initialize()
                val odometer = vehicle.getOdometer()
                odometer.checkCurrentDistanceDrivenCapabilities(onComplete = { capability ->
                    if (capability.accessType != AccessType.READ && capability.accessType != AccessType.READ_WRITE) latch.countDown()
                    else try {
                        odometer.getCurrentDistanceDriven(onComplete = { distance ->
                            value.set(distance?.toDouble()?.takeIf { it.isFinite() && it >= 0 })
                            latch.countDown()
                        }, onFailed = { latch.countDown() })
                    } catch (_: Exception) { latch.countDown() }
                }, onFailed = { latch.countDown() })
                latch.await(8, java.util.concurrent.TimeUnit.SECONDS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (_: Exception) {
                // Date-based reminders still work if the vehicle service is unavailable.
            } finally {
                runCatching { vehicle?.release() }
            }
            if (!Thread.currentThread().isInterrupted) {
                MaintenanceReminders.notifyDue(this, value.get())
                jobFinished(params, false)
            }
        }
        worker = task
        task.start()
        return true
    }
    override fun onStopJob(params: JobParameters?): Boolean { worker?.interrupt(); worker = null; return true }
}
