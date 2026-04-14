package com.yourteam.nextstop.ui.driver;

import android.content.Context;
import com.yourteam.nextstop.data.repository.DriverRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class DriverViewModel_Factory implements Factory<DriverViewModel> {
  private final Provider<Context> appContextProvider;

  private final Provider<DriverRepository> driverRepositoryProvider;

  public DriverViewModel_Factory(Provider<Context> appContextProvider,
      Provider<DriverRepository> driverRepositoryProvider) {
    this.appContextProvider = appContextProvider;
    this.driverRepositoryProvider = driverRepositoryProvider;
  }

  @Override
  public DriverViewModel get() {
    return newInstance(appContextProvider.get(), driverRepositoryProvider.get());
  }

  public static DriverViewModel_Factory create(Provider<Context> appContextProvider,
      Provider<DriverRepository> driverRepositoryProvider) {
    return new DriverViewModel_Factory(appContextProvider, driverRepositoryProvider);
  }

  public static DriverViewModel newInstance(Context appContext, DriverRepository driverRepository) {
    return new DriverViewModel(appContext, driverRepository);
  }
}
