package com.example.avisaki;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;


public class Services extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        acionar();
        configurarWorkManager();
    }

    private void acionar() {
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AlertaWorker.class)
                .build();

        WorkManager.getInstance(this).enqueue(workRequest);
    }

    private void configurarWorkManager() {
        // Criação de uma requisição de trabalho periódico
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(AlertaWorker.class, 1, TimeUnit.HOURS)
                .build();

        // Agendar o trabalho
        WorkManager.getInstance(this).enqueue(workRequest);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

