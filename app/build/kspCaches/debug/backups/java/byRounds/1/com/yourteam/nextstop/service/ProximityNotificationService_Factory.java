package com.yourteam.nextstop.service;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class ProximityNotificationService_Factory implements Factory<ProximityNotificationService> {
  private final Provider<Context> contextProvider;

  public ProximityNotificationService_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ProximityNotificationService get() {
    return newInstance(contextProvider.get());
  }

  public static ProximityNotificationService_Factory create(Provider<Context> contextProvider) {
    return new ProximityNotificationService_Factory(contextProvider);
  }

  public static ProximityNotificationService newInstance(Context context) {
    return new ProximityNotificationService(context);
  }
}
