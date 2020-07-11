package com.android.master.kyc

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class MyApplication : Application() {
    private val dataModule = module {
//        single {
//            Room.databaseBuilder(androidContext(), FilmsDatabase::class.java, DATABASE_NAME)
//                .allowMainThreadQueries() // allow querying on MainThread (this useful in some cases)
//                .fallbackToDestructiveMigration() //  this mean that it will delete all tables and recreate them after version is changed
//                .build()
//        }
//
//        single { FilmRepository(get<FilmsDatabase>().getFilmDao()) }
//        single { ImageRepository(get<FilmsDatabase>().getImageDao()) }
    }


    override fun onCreate() {
        super.onCreate()
        // start Koin!
        startKoin {
            // declare used Android context
            androidContext(this@MyApplication)
            // declare modules
            modules(listOf(dataModule))
        }
    }
}