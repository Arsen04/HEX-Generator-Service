package com.protelion.hexclient.di

import androidx.room.Room
import com.protelion.hexclient.data.local.AppDatabase
import com.protelion.hexclient.data.repository.HexRepositoryImpl
import com.protelion.hexclient.domain.repository.HexRepository
import com.protelion.hexclient.domain.usecase.GenerateHexUseCase
import com.protelion.hexclient.presentation.viewmodel.MainViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database & DAO
    single {
        Room.databaseBuilder(
            androidApplication(),
            AppDatabase::class.java,
            "hex_database"
        ).build()
    }
    single { get<AppDatabase>().hexDao() }

    // Repository
    single<HexRepository> { HexRepositoryImpl(get()) }

    // UseCase
    single { GenerateHexUseCase() }

    // ViewModel
    viewModel { MainViewModel(androidApplication(), get()) }
}
