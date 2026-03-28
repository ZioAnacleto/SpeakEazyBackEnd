package com.zioanacleto.admin

class EnvironmentKeysProviderImpl : EnvironmentKeysProvider {
    override fun provideKey(key: EnvironmentKey): String = System.getenv(key.key)
}