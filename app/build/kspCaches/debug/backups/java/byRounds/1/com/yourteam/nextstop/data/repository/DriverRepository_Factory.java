package com.yourteam.nextstop.data.repository;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class DriverRepository_Factory implements Factory<DriverRepository> {
  private final Provider<FirebaseAuth> authProvider;

  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseDatabase> realtimeDbProvider;

  public DriverRepository_Factory(Provider<FirebaseAuth> authProvider,
      Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseDatabase> realtimeDbProvider) {
    this.authProvider = authProvider;
    this.firestoreProvider = firestoreProvider;
    this.realtimeDbProvider = realtimeDbProvider;
  }

  @Override
  public DriverRepository get() {
    return newInstance(authProvider.get(), firestoreProvider.get(), realtimeDbProvider.get());
  }

  public static DriverRepository_Factory create(Provider<FirebaseAuth> authProvider,
      Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseDatabase> realtimeDbProvider) {
    return new DriverRepository_Factory(authProvider, firestoreProvider, realtimeDbProvider);
  }

  public static DriverRepository newInstance(FirebaseAuth auth, FirebaseFirestore firestore,
      FirebaseDatabase realtimeDb) {
    return new DriverRepository(auth, firestore, realtimeDb);
  }
}
