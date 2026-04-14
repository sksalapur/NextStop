package com.yourteam.nextstop.service;

import com.yourteam.nextstop.data.repository.DriverRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@QualifierMetadata
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
public final class LocationForegroundService_MembersInjector implements MembersInjector<LocationForegroundService> {
  private final Provider<DriverRepository> driverRepositoryProvider;

  public LocationForegroundService_MembersInjector(
      Provider<DriverRepository> driverRepositoryProvider) {
    this.driverRepositoryProvider = driverRepositoryProvider;
  }

  public static MembersInjector<LocationForegroundService> create(
      Provider<DriverRepository> driverRepositoryProvider) {
    return new LocationForegroundService_MembersInjector(driverRepositoryProvider);
  }

  @Override
  public void injectMembers(LocationForegroundService instance) {
    injectDriverRepository(instance, driverRepositoryProvider.get());
  }

  @InjectedFieldSignature("com.yourteam.nextstop.service.LocationForegroundService.driverRepository")
  public static void injectDriverRepository(LocationForegroundService instance,
      DriverRepository driverRepository) {
    instance.driverRepository = driverRepository;
  }
}
