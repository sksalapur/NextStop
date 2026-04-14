package com.yourteam.nextstop.ui.admin;

import com.yourteam.nextstop.data.repository.AdminRepository;
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
public final class AdminViewModel_Factory implements Factory<AdminViewModel> {
  private final Provider<AdminRepository> repositoryProvider;

  public AdminViewModel_Factory(Provider<AdminRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public AdminViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static AdminViewModel_Factory create(Provider<AdminRepository> repositoryProvider) {
    return new AdminViewModel_Factory(repositoryProvider);
  }

  public static AdminViewModel newInstance(AdminRepository repository) {
    return new AdminViewModel(repository);
  }
}
