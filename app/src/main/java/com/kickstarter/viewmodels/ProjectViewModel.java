package com.kickstarter.viewmodels;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Pair;

import com.kickstarter.KSApplication;
import com.kickstarter.libs.CurrentUser;
import com.kickstarter.libs.ViewModel;
import com.kickstarter.libs.rx.transformers.Transformers;
import com.kickstarter.models.Project;
import com.kickstarter.models.Reward;
import com.kickstarter.models.User;
import com.kickstarter.services.ApiClient;
import com.kickstarter.ui.activities.ProjectActivity;
import com.kickstarter.ui.adapters.ProjectAdapter;
import com.kickstarter.ui.viewholders.ProjectViewHolder;
import com.kickstarter.ui.viewholders.RewardViewHolder;
import com.kickstarter.viewmodels.inputs.ProjectViewModelInputs;
import com.kickstarter.viewmodels.outputs.ProjectViewModelOutputs;

import javax.inject.Inject;

import rx.Observable;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public final class ProjectViewModel extends ViewModel<ProjectActivity> implements ProjectAdapter.Delegate, ProjectViewModelInputs, ProjectViewModelOutputs {
  @Inject ApiClient client;
  @Inject CurrentUser currentUser;

  // INPUTS
  private final PublishSubject<String> initialProjectParam = PublishSubject.create();
  public void initialProjectParam(@NonNull String param) {
    this.initialProjectParam.onNext(param);
  }
  private final PublishSubject<Project> initialProject = PublishSubject.create();
  public void initialProject(@NonNull Project project) {
    this.initialProject.onNext(project);
  }
  private final PublishSubject<Void> backProjectClicked = PublishSubject.create();
  public void backProjectClicked() {
    this.backProjectClicked.onNext(null);
  }
  private final PublishSubject<Void> shareClicked = PublishSubject.create();
  public void shareClicked() {
    this.shareClicked.onNext(null);
  }
  private final PublishSubject<Void> blurbClicked = PublishSubject.create();
  public void blurbClicked() {
    this.blurbClicked.onNext(null);
  }
  private final PublishSubject<Void> commentsClicked = PublishSubject.create();
  public void commentsClicked() {
    this.commentsClicked.onNext(null);
  }
  private final PublishSubject<Void> creatorNameClicked = PublishSubject.create();
  public void creatorNameClicked() {
    this.creatorNameClicked.onNext(null);
  }
  private final PublishSubject<Void> managePledgeClicked = PublishSubject.create();
  public void managePledgeClicked() {
    this.managePledgeClicked.onNext(null);
  }
  private final PublishSubject<Void> updatesClicked = PublishSubject.create();
  public void updatesClicked() {
    this.updatesClicked.onNext(null);
  }
  private final PublishSubject<Void> playVideoClicked = PublishSubject.create();
  public void playVideoClicked() {
    this.playVideoClicked.onNext(null);
  }
  private final PublishSubject<Void> viewPledgeClicked = PublishSubject.create();
  public void viewPledgeClicked() {
    this.viewPledgeClicked.onNext(null);
  }
  private final PublishSubject<Void> starClicked = PublishSubject.create();
  public void starClicked() {
    this.starClicked.onNext(null);
  }
  private final PublishSubject<Reward> rewardClicked = PublishSubject.create();
  public void rewardClicked(final @NonNull Reward reward) {
    this.rewardClicked.onNext(reward);
  }
  private final PublishSubject<Void> loginSuccess = PublishSubject.create();
  public void loginSuccess() {
    this.loginSuccess.onNext(null);
  }
  public final ProjectViewModelInputs inputs = this;

  // OUTPUTS
  final BehaviorSubject<Project> project = BehaviorSubject.create();
  public Observable<Project> project() {
    return this.project;
  }
  public Observable<Project> showShareSheet() {
    return this.project.compose(Transformers.takeWhen(this.shareClicked));
  }
  public Observable<Project> playVideo() {
    return this.project.compose(Transformers.takeWhen(this.playVideoClicked));
  }
  public Observable<Project> showCampaign() {
    return this.project.compose(Transformers.takeWhen(this.blurbClicked));
  }
  public Observable<Project> showCreator() {
    return this.project.compose(Transformers.takeWhen(this.creatorNameClicked));
  }
  public Observable<Project> showUpdates() {
    return this.project.compose(Transformers.takeWhen(this.updatesClicked));
  }
  public Observable<Project> showComments() {
    return this.project.compose(Transformers.takeWhen(this.commentsClicked));
  }
  public Observable<Project> startCheckout() {
    return this.project.compose(Transformers.takeWhen(this.backProjectClicked));
  }
  public Observable<Project> startManagePledge() {
    return this.project.compose(Transformers.takeWhen(this.managePledgeClicked));
  }
  public Observable<Project> startViewPledge() {
    return this.project.compose(Transformers.takeWhen(this.viewPledgeClicked));
  }
  public Observable<Pair<Project, Reward>> startCheckoutWithReward() {
    return this.project.compose(Transformers.takePairWhen(this.rewardClicked));
  }
  private final PublishSubject<Void> showStarredPrompt = PublishSubject.create();
  public Observable<Void> showStarredPrompt() {
    return this.showStarredPrompt;
  }
  private final PublishSubject<Void> showLoginTout = PublishSubject.create();
  public Observable<Void> showLoginTout() {
    return this.showLoginTout;
  }
  public final ProjectViewModelOutputs outputs = this;

  @Override
  protected void onCreate(@NonNull final Context context, @Nullable final Bundle savedInstanceState) {
    super.onCreate(context, savedInstanceState);
    ((KSApplication) context.getApplicationContext()).component().inject(this);

    final Observable<User> loggedInUserOnStarClick = currentUser.observable()
      .compose(Transformers.takeWhen(starClicked))
      .filter(u -> u != null);

    final Observable<User> loggedOutUserOnStarClick = currentUser.observable()
      .compose(Transformers.takeWhen(starClicked))
      .filter(u -> u == null);

    final Observable<Project> projectOnUserChangeStar = initialProject
      .compose(Transformers.takeWhen(loggedInUserOnStarClick))
      .switchMap(this::toggleProjectStar)
      .share();

    final Observable<Project> starredProjectOnLoginSuccess = initialProject
      .compose(Transformers.takeWhen(loginSuccess))
      .take(1)
      .switchMap(this::starProject)
      .share();

    addSubscription(
      this.initialProject.map(Project::param).mergeWith(this.initialProjectParam)
        .filter(param -> param != null)
        .switchMap(param -> client.fetchProject(param).compose(Transformers.neverError()))
        .mergeWith(projectOnUserChangeStar)
        .mergeWith(starredProjectOnLoginSuccess)
        .mergeWith(initialProject)
        .subscribe(this.project::onNext)
    );

    addSubscription(
      projectOnUserChangeStar.mergeWith(starredProjectOnLoginSuccess)
        .filter(Project::isStarred)
        .subscribe(__ -> this.showStarredPrompt.onNext(null))
    );

    addSubscription(
      loggedOutUserOnStarClick.subscribe(__ ->
        this.showLoginTout.onNext(null)
      )
    );

    addSubscription(shareClicked.subscribe(__ -> koala.trackShowProjectShareSheet()));

    addSubscription(projectOnUserChangeStar.mergeWith(starredProjectOnLoginSuccess)
      .subscribe(koala::trackProjectStar));

    koala.trackProjectShow();
  }

  public void projectViewHolderBlurbClicked(@NonNull final ProjectViewHolder viewHolder) {
    this.blurbClicked();
  }

  public void projectViewHolderCommentsClicked(@NonNull final ProjectViewHolder viewHolder) {
    this.commentsClicked();
  }

  public void projectViewHolderCreatorClicked(@NonNull final ProjectViewHolder viewHolder){
    this.creatorNameClicked();
  }

  public void rewardViewHolderClicked(@NonNull final RewardViewHolder viewHolder, @NonNull final Reward reward) {
    this.rewardClicked(reward);
  }

  public void projectViewHolderVideoStarted(@NonNull final ProjectViewHolder viewHolder) {
    this.playVideoClicked();
  }

  public void projectViewHolderUpdatesClicked(@NonNull final ProjectViewHolder viewHolder) {
    this.updatesClicked();
  }

  public Observable<Project> starProject(@NonNull final Project project) {
    return client.starProject(project)
      .onErrorResumeNext(Observable.empty());
  }

  public Observable<Project> toggleProjectStar(@NonNull final Project project) {
    return client.toggleProjectStar(project)
      .onErrorResumeNext(Observable.empty());
  }
}
