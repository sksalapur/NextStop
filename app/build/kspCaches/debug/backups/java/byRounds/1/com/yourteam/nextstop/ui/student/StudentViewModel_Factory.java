package com.yourteam.nextstop.ui.student;

import com.yourteam.nextstop.data.repository.StudentRepository;
import com.yourteam.nextstop.service.ProximityNotificationService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class StudentViewModel_Factory implements Factory<StudentViewModel> {
  private final Provider<StudentRepository> repositoryProvider;

  private final Provider<ProximityNotificationService> notificationServiceProvider;

  public StudentViewModel_Factory(Provider<StudentRepository> repositoryProvider,
      Provider<ProximityNotificationService> notificationServiceProvider) {
    this.repositoryProvider = repositoryProvider;
    this.notificationServiceProvider = notificationServiceProvider;
  }

  @Override
  public StudentViewModel get() {
    return newInstance(repositoryProvider.get(), notificationServiceProvider.get());
  }

  public static StudentViewModel_Factory create(Provider<StudentRepository> repositoryProvider,
      Provider<ProximityNotificationService> notificationServiceProvider) {
    return new StudentViewModel_Factory(repositoryProvider, notificationServiceProvider);
  }

  public static StudentViewModel newInstance(StudentRepository repository,
      ProximityNotificationService notificationService) {
    return new StudentViewModel(repository, notificationService);
  }
}
