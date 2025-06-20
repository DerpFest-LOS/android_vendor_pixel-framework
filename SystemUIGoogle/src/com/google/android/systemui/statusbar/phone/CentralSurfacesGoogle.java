/*
 * Copyright (C) 2022 The PixelExperience Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.systemui.statusbar.phone;

import static com.android.systemui.Dependency.TIME_TICK_HANDLER_NAME;

import android.app.WallpaperManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.devicestate.DeviceStateManager;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.service.dreams.IDreamManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.app.viewcapture.ViewCaptureAwareWindowManager;
import com.android.internal.logging.MetricsLogger;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.ViewMediatorCallback;
import com.android.systemui.accessibility.floatingmenu.AccessibilityFloatingMenuController;
import com.android.systemui.animation.ActivityTransitionAnimator;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.back.domain.interactor.BackActionInteractor;
import com.android.systemui.biometrics.AuthRippleController;
import com.android.systemui.bouncer.domain.interactor.AlternateBouncerInteractor;
import com.android.systemui.settings.brightness.domain.interactor.BrightnessMirrorShowingInteractor;
import com.android.systemui.broadcast.BroadcastDispatcher;
import com.android.systemui.charging.WiredChargingRippleController;
import com.android.systemui.classifier.FalsingCollector;
import com.android.systemui.colorextraction.SysuiColorExtractor;
import com.android.systemui.communal.domain.interactor.CommunalInteractor;
import com.android.systemui.dagger.qualifiers.Main;
import com.android.systemui.dagger.qualifiers.UiBackground;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.demomode.DemoModeController;
import com.android.systemui.emergency.EmergencyGestureModule.EmergencyGestureIntentFactory;
import com.android.systemui.flags.FeatureFlags;
import com.android.systemui.fragments.FragmentService;
import com.android.systemui.InitController;
import com.android.systemui.keyguard.KeyguardUnlockAnimationController;
import com.android.systemui.keyguard.KeyguardViewMediator;
import com.android.systemui.keyguard.ScreenLifecycle;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.navigationbar.NavigationBarController;
import com.android.systemui.notetask.NoteTaskController;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.PluginDependencyProvider;
import com.android.systemui.plugins.PluginManager;
import com.android.systemui.power.domain.interactor.PowerInteractor;
import com.android.systemui.recents.ScreenPinningRequest;
import com.google.android.systemui.res.R;
import com.android.systemui.scene.domain.interactor.WindowRootViewVisibilityInteractor;
import com.android.systemui.settings.brightness.BrightnessSliderController;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.shade.CameraLauncher;
import com.android.systemui.shade.GlanceableHubContainerController;
import com.android.systemui.shade.NotificationPanelViewController;
import com.android.systemui.shade.NotificationShadeWindowView;
import com.android.systemui.shade.NotificationShadeWindowViewController;
import com.android.systemui.shade.QuickSettingsController;
import com.android.systemui.shade.ShadeController;
import com.android.systemui.shade.ShadeExpansionStateManager;
import com.android.systemui.shade.ShadeLogger;
import com.android.systemui.shade.ShadeSurface;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.core.StatusBarInitializer;
import com.android.systemui.statusbar.data.repository.StatusBarModeRepositoryStore;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.LightRevealScrim;
import com.android.systemui.statusbar.LockscreenShadeTransitionController;
import com.android.systemui.statusbar.notification.headsup.HeadsUpManager;
import com.android.systemui.statusbar.notification.collection.NotifPipeline;
import com.android.systemui.statusbar.notification.init.NotificationsController;
import com.android.systemui.statusbar.notification.interruption.NotificationInterruptStateProvider;
import com.android.systemui.statusbar.notification.logging.NotificationLogger;
import com.android.systemui.statusbar.notification.NotificationActivityStarter;
import com.android.systemui.statusbar.notification.NotificationLaunchAnimatorControllerProvider;
import com.android.systemui.statusbar.notification.NotificationLaunchAnimatorControllerProvider;
import com.android.systemui.statusbar.notification.NotificationWakeUpCoordinator;
import com.android.systemui.statusbar.notification.row.NotificationGutsManager;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayoutController;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.NotificationPresenter;
import com.android.systemui.statusbar.NotificationRemoteInputManager;
import com.android.systemui.statusbar.NotificationShadeDepthController;
import com.android.systemui.statusbar.NotificationShadeWindowController;
import com.android.systemui.statusbar.phone.*;
import com.android.systemui.statusbar.phone.AutoHideController;
import com.android.systemui.statusbar.phone.BiometricUnlockController;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.phone.DozeScrimController;
import com.android.systemui.statusbar.phone.DozeServiceHost;
import com.android.systemui.statusbar.phone.KeyguardBypassController;
import com.android.systemui.statusbar.phone.LightBarController;
import com.android.systemui.statusbar.phone.PhoneStatusBarPolicy;
import com.android.systemui.statusbar.phone.ShadeTouchableRegionManager;
import com.android.systemui.statusbar.phone.StatusBarHideIconsForBouncerManager;
import com.android.systemui.statusbar.phone.StatusBarKeyguardViewManager;
import com.android.systemui.statusbar.phone.StatusBarSignalPolicy;
import com.android.systemui.statusbar.phone.ongoingcall.OngoingCallController;
import com.android.systemui.statusbar.policy.BatteryController;
import com.android.systemui.statusbar.policy.BurnInProtectionController;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.ExtensionController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.statusbar.policy.UserInfoControllerImpl;
import com.android.systemui.statusbar.PulseExpansionHandler;
import com.android.systemui.statusbar.SysuiStatusBarStateController;
import com.android.systemui.statusbar.window.StatusBarWindowControllerStore;
import com.android.systemui.statusbar.window.StatusBarWindowStateController;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.concurrency.DelayableExecutor;
import com.android.systemui.util.concurrency.MessageRouter;
import com.android.systemui.util.kotlin.JavaAdapter;
import com.android.systemui.util.WallpaperController;
import com.android.systemui.volume.VolumeComponent;
import com.android.wm.shell.bubbles.Bubbles;
import com.android.wm.shell.startingsurface.StartingSurface;

import com.google.android.systemui.NotificationLockscreenUserManagerGoogle;
import com.google.android.systemui.dreamliner.DockIndicationController;
import com.google.android.systemui.dreamliner.DockObserver;
import com.google.android.systemui.reversecharging.ReverseChargingViewController;
import com.google.android.systemui.smartspace.SmartSpaceController;
import com.google.android.systemui.statusbar.KeyguardIndicationControllerGoogle;

import org.sun.systemui.statusbar.ticker.TickerController;

import java.util.Optional;
import java.util.concurrent.Executor;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import dagger.Lazy;

@SysUISingleton
public class CentralSurfacesGoogle extends CentralSurfacesImpl {

    private static final boolean DEBUG = Log.isLoggable("CentralSurfacesGoogle", 3);
    private final BatteryController.BatteryStateChangeCallback mBatteryStateChangeCallback;
    private final KeyguardIndicationControllerGoogle mKeyguardIndicationController;
    private final WallpaperNotifier mWallpaperNotifier;
    private final Optional<ReverseChargingViewController> mReverseChargingViewControllerOptional;
    private final SysuiStatusBarStateController mStatusBarStateController;
    private final SmartSpaceController mSmartSpaceController;
    private final NotificationLockscreenUserManagerGoogle mNotificationLockscreenUserManagerGoogle;
    private final DockObserver mDockObserver;
    private final NotificationPanelViewController mNotificationPanelViewController;
    private final TickerController mTickerController;
    private final Lazy<NotificationPanelViewController> mPanelViewControllerLazy;
    private final NotifPipeline mNotifPipeline;
    private final NotificationInterruptStateProvider mNotificationInterruptStateProvider;

    private long mAnimStartTime;
    private int mReceivingBatteryLevel;
    private boolean mReverseChargingAnimShown;
    private boolean mChargingAnimShown;
    private Context mContext;

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @Inject
    public CentralSurfacesGoogle(
            Context context,
            NotificationsController notificationsController,
            FragmentService fragmentService,
            LightBarController lightBarController,
            AutoHideController autoHideController,
            StatusBarInitializer statusBarInitializer,
            StatusBarWindowControllerStore statusBarWindowControllerStore,
            StatusBarWindowStateController statusBarWindowStateController,
            StatusBarModeRepositoryStore statusBarModeRepository,
            KeyguardUpdateMonitor keyguardUpdateMonitor,
            StatusBarSignalPolicy statusBarSignalPolicy,
            PulseExpansionHandler pulseExpansionHandler,
            NotificationWakeUpCoordinator notificationWakeUpCoordinator,
            KeyguardBypassController keyguardBypassController,
            KeyguardStateController keyguardStateController,
            HeadsUpManager headsUpManager,
            FalsingManager falsingManager,
            FalsingCollector falsingCollector,
            BroadcastDispatcher broadcastDispatcher,
            NotificationGutsManager notificationGutsManager,
            ShadeExpansionStateManager shadeExpansionStateManager,
            KeyguardViewMediator keyguardViewMediator,
            DisplayMetrics displayMetrics,
            MetricsLogger metricsLogger,
            ShadeLogger shadeLogger,
            JavaAdapter javaAdapter,
            @UiBackground Executor uiBgExecutor,
            ShadeSurface shadeSurface,
            NotificationMediaManager notificationMediaManager,
            NotificationLockscreenUserManagerGoogle notificationLockscreenUserManagerGoogle,
            NotificationRemoteInputManager remoteInputManager,
            QuickSettingsController quickSettingsController,
            BatteryController batteryController,
            SysuiColorExtractor colorExtractor,
            ScreenLifecycle screenLifecycle,
            WakefulnessLifecycle wakefulnessLifecycle,
            PowerInteractor powerInteractor,
            CommunalInteractor communalInteractor,
            SysuiStatusBarStateController statusBarStateController,
            Optional<Bubbles> bubblesOptional,
            Lazy<NoteTaskController> noteTaskControllerLazy,
            DeviceProvisionedController deviceProvisionedController,
            NavigationBarController navigationBarController,
            AccessibilityFloatingMenuController accessibilityFloatingMenuController,
            Lazy<AssistManager> assistManagerLazy,
            // TODO: b/374267505 - Decouple the config change needed for shade window classes from
            //  the one for other windows.
            ConfigurationController configurationController,
            NotificationShadeWindowController notificationShadeWindowController,
            Lazy<NotificationShadeWindowViewController> notificationShadeWindowViewControllerLazy,
            NotificationStackScrollLayoutController notificationStackScrollLayoutController,
            // Lazys due to b/298099682.
            Lazy<NotificationPresenter> notificationPresenterLazy,
            Lazy<NotificationActivityStarter> notificationActivityStarterLazy,
            NotificationLaunchAnimatorControllerProvider notifTransitionAnimatorControllerProvider,
            DozeParameters dozeParameters,
            ScrimController scrimController,
            Lazy<BiometricUnlockController> biometricUnlockControllerLazy,
            AuthRippleController authRippleController,
            DozeServiceHost dozeServiceHost,
            BackActionInteractor backActionInteractor,
            PowerManager powerManager,
            DozeScrimController dozeScrimController,
            VolumeComponent volumeComponent,
            CommandQueue commandQueue,
            Lazy<CentralSurfacesCommandQueueCallbacks> commandQueueCallbacksLazy,
            PluginManager pluginManager,
            ShadeController shadeController,
            WindowRootViewVisibilityInteractor windowRootViewVisibilityInteractor,
            StatusBarKeyguardViewManager statusBarKeyguardViewManager,
            ViewMediatorCallback viewMediatorCallback,
            InitController initController,
            @Named(TIME_TICK_HANDLER_NAME) Handler timeTickHandler,
            PluginDependencyProvider pluginDependencyProvider,
            ExtensionController extensionController,
            UserInfoControllerImpl userInfoControllerImpl,
            PhoneStatusBarPolicy phoneStatusBarPolicy,
            KeyguardIndicationControllerGoogle keyguardIndicationControllerGoogle,
            DemoModeController demoModeController,
            Lazy<NotificationShadeDepthController> notificationShadeDepthControllerLazy,
            ShadeTouchableRegionManager shadeTouchableRegionManager,
            BrightnessSliderController.Factory brightnessSliderFactory,
            ScreenOffAnimationController screenOffAnimationController,
            WallpaperController wallpaperController,
            StatusBarHideIconsForBouncerManager statusBarHideIconsForBouncerManager,
            LockscreenShadeTransitionController lockscreenShadeTransitionController,
            FeatureFlags featureFlags,
            KeyguardUnlockAnimationController keyguardUnlockAnimationController,
            @Main DelayableExecutor delayableExecutor,
            @Main MessageRouter messageRouter,
            WallpaperManager wallpaperManager,
            Optional<StartingSurface> startingSurfaceOptional,
            ActivityTransitionAnimator activityTransitionAnimator,
            DeviceStateManager deviceStateManager,
            WiredChargingRippleController wiredChargingRippleController,
            IDreamManager dreamManager,
            Lazy<CameraLauncher> cameraLauncherLazy,
            LightRevealScrim lightRevealScrim,
            AlternateBouncerInteractor alternateBouncerInteractor,
            UserTracker userTracker,
            Provider<FingerprintManager> fingerprintManager,
            TunerService tunerService,
            ActivityStarter activityStarter,
            BrightnessMirrorShowingInteractor brightnessMirrorShowingInteractor,
            GlanceableHubContainerController glanceableHubContainerController,
            EmergencyGestureIntentFactory emergencyGestureIntentFactory,
            ViewCaptureAwareWindowManager viewCaptureAwareWindowManager,
            BurnInProtectionController burnInProtectionController,
            Optional<ReverseChargingViewController> reverseChargingViewControllerOptional,
            WallpaperNotifier wallpaperNotifier,
            SmartSpaceController smartSpaceController,
            DockObserver dockObserver,
            NotificationPanelViewController notificationPanelViewController,
            TickerController tickerController,
            Lazy<NotificationPanelViewController> panelViewControllerLazy,
            NotifPipeline notifPipeline,
            NotificationInterruptStateProvider notificationInterruptStateProvider

    ) {
        super(context, notificationsController, fragmentService, lightBarController, autoHideController, statusBarInitializer,
                statusBarWindowControllerStore, statusBarWindowStateController, statusBarModeRepository, keyguardUpdateMonitor,
                statusBarSignalPolicy, pulseExpansionHandler, notificationWakeUpCoordinator, keyguardBypassController, keyguardStateController,
                headsUpManager, falsingManager, falsingCollector, broadcastDispatcher, notifPipeline, notificationGutsManager, shadeExpansionStateManager, tickerController, keyguardViewMediator, displayMetrics, metricsLogger, shadeLogger,
                javaAdapter, uiBgExecutor, shadeSurface, notificationMediaManager, notificationLockscreenUserManagerGoogle, remoteInputManager,
                quickSettingsController, batteryController, colorExtractor, screenLifecycle, wakefulnessLifecycle,
                powerInteractor, communalInteractor, statusBarStateController, bubblesOptional, noteTaskControllerLazy, deviceProvisionedController,
                navigationBarController, accessibilityFloatingMenuController, assistManagerLazy, configurationController, notificationShadeWindowController,
                panelViewControllerLazy, notificationShadeWindowViewControllerLazy, notificationStackScrollLayoutController, notificationPresenterLazy, notificationActivityStarterLazy,
                notifTransitionAnimatorControllerProvider, dozeParameters, scrimController, biometricUnlockControllerLazy, authRippleController,
                dozeServiceHost, backActionInteractor, powerManager, dozeScrimController, volumeComponent, commandQueue, commandQueueCallbacksLazy,
                pluginManager, shadeController, windowRootViewVisibilityInteractor, statusBarKeyguardViewManager, viewMediatorCallback, initController, timeTickHandler,
                pluginDependencyProvider, extensionController, userInfoControllerImpl, phoneStatusBarPolicy, keyguardIndicationControllerGoogle, demoModeController,
                notificationShadeDepthControllerLazy, shadeTouchableRegionManager, notificationInterruptStateProvider, brightnessSliderFactory, screenOffAnimationController, wallpaperController, statusBarHideIconsForBouncerManager, lockscreenShadeTransitionController, featureFlags,
                keyguardUnlockAnimationController, delayableExecutor, messageRouter, wallpaperManager, startingSurfaceOptional, activityTransitionAnimator,
                deviceStateManager, wiredChargingRippleController, dreamManager, cameraLauncherLazy, lightRevealScrim,
                alternateBouncerInteractor, userTracker, fingerprintManager, tunerService, activityStarter, brightnessMirrorShowingInteractor,
                glanceableHubContainerController, emergencyGestureIntentFactory, viewCaptureAwareWindowManager, burnInProtectionController,
                notificationPanelViewController);
        mContext = context;
        mBatteryStateChangeCallback = new BatteryController.BatteryStateChangeCallback() {
            @Override
            public void onBatteryLevelChanged(int i, boolean z, boolean z2) {
                mReceivingBatteryLevel = i;
                if (!mBatteryController.isWirelessCharging()) {
                    if (SystemClock.uptimeMillis() - mAnimStartTime > 1500) {
                        mChargingAnimShown = false;
                    }
                    mReverseChargingAnimShown = false;
                }
                if (DEBUG) {
                    Log.d("CentralSurfacesGoogle", "onBatteryLevelChanged(): level=" + i + ",wlc=" + (mBatteryController.isWirelessCharging() ? 1 : 0) + ",wlcs=" + mChargingAnimShown + ",rtxs=" + mReverseChargingAnimShown + ",this=" + this);
                }
            }

            @Override
            public void onReverseChanged(boolean z, int i, String str) {
                if (!z && i >= 0 && !TextUtils.isEmpty(str) && mBatteryController.isWirelessCharging() && mChargingAnimShown && !mReverseChargingAnimShown) {
                    mReverseChargingAnimShown = true;
                    long uptimeMillis = SystemClock.uptimeMillis() - mAnimStartTime;
                    long j = uptimeMillis > 1500 ? 0L : 1500 - uptimeMillis;
                    showChargingAnimation(mReceivingBatteryLevel, i, j);
                }
                if (DEBUG) {
                    Log.d("CentralSurfacesGoogle", "onReverseChanged(): rtx=" + (z ? 1 : 0) + ",rxlevel=" + mReceivingBatteryLevel + ",level=" + i + ",name=" + str + ",wlc=" + (mBatteryController.isWirelessCharging() ? 1 : 0) + ",wlcs=" + mChargingAnimShown + ",rtxs=" + mReverseChargingAnimShown + ",this=" + this);
                }
            }
        };
        mReverseChargingViewControllerOptional = reverseChargingViewControllerOptional;
        mKeyguardIndicationController = keyguardIndicationControllerGoogle;
        mStatusBarStateController = statusBarStateController;
        mWallpaperNotifier = wallpaperNotifier;
        mSmartSpaceController = smartSpaceController;
        mNotificationLockscreenUserManagerGoogle = notificationLockscreenUserManagerGoogle;
        mDockObserver = dockObserver;
        mNotificationPanelViewController = notificationPanelViewController;
        mTickerController = tickerController;
        mPanelViewControllerLazy = panelViewControllerLazy;
        mNotifPipeline = notifPipeline;
        mNotificationInterruptStateProvider = notificationInterruptStateProvider;
    }

    @Override
    public void start() {
        super.start();
        mWallpaperNotifier.attach();
        mBatteryController.observe(getLifecycle(), mBatteryStateChangeCallback);
        mDockObserver.setDreamlinerGear((ImageView) getNotificationShadeWindowView().findViewById(R.id.dreamliner_gear));
        mDockObserver.setPhotoPreview((FrameLayout) getNotificationShadeWindowView().findViewById(R.id.photo_preview));
        mDockObserver.setIndicationController(new DockIndicationController(mContext, mKeyguardIndicationController, mStatusBarStateController, this));
        mDockObserver.registerDockAlignInfo();
        if (mReverseChargingViewControllerOptional.isPresent()) {
            mReverseChargingViewControllerOptional.get().initialize();
        }
        mNotificationLockscreenUserManagerGoogle.updateSmartSpaceVisibilitySettings();
    }

    @Override
    public void showWirelessChargingAnimation(int i) {
        if (DEBUG) {
            Log.d("CentralSurfacesGoogle", "showWirelessChargingAnimation()");
        }
        mChargingAnimShown = true;
        super.showWirelessChargingAnimation(i);
        mAnimStartTime = SystemClock.uptimeMillis();
    }
}
