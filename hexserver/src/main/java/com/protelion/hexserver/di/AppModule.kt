package com.protelion.hexserver.di

import androidx.room.Room
import com.protelion.hexserver.data.local.ServiceDatabase
import com.protelion.hexserver.domain.usecase.GenerateHexUseCase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            ServiceDatabase::class.java,
            "service_database"
        ).build()
    }
    single { get<ServiceDatabase>().serviceHexDao() }
    single { GenerateHexUseCase() }
}
