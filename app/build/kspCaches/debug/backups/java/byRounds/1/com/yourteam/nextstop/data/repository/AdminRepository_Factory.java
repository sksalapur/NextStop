package com.yourteam.nextstop.data.repository;

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
public final class AdminRepository_Factory implements Factory<AdminRepository> {
  private final Provider<FirebaseFirestore> firestoreProvider;

  private final Provider<FirebaseDatabase> realtimeDbProvider;

  public AdminRepository_Factory(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseDatabase> realtimeDbProvider) {
    this.firestoreProvider = firestoreProvider;
    this.realtimeDbProvider = realtimeDbProvider;
  }

  @Override
  public AdminRepository get() {
    return newInstance(firestoreProvider.get(), realtimeDbProvider.get());
  }

  public static AdminRepository_Factory create(Provider<FirebaseFirestore> firestoreProvider,
      Provider<FirebaseDatabase> realtimeDbProvider) {
    return new AdminRepository_Factory(firestoreProvider, realtimeDbProvider);
  }

  public static AdminRepository newInstance(FirebaseFirestore firestore,
      FirebaseDatabase realtimeDb) {
    return new AdminRepository(firestore, realtimeDb);
  }
}
