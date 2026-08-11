package gr.larryngton.demise.features.modules;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.misc.GameEvent;
import gr.larryngton.demise.events.impl.misc.KeyPressEvent;
import gr.larryngton.demise.features.modules.impl.combat.*;
import gr.larryngton.demise.features.modules.impl.exploit.*;
import gr.larryngton.demise.features.modules.impl.exploit.Timer;
import gr.larryngton.demise.features.modules.impl.legit.*;
import gr.larryngton.demise.features.modules.impl.misc.*;
import gr.larryngton.demise.features.modules.impl.movement.*;
import gr.larryngton.demise.features.modules.impl.client.ClientSettings;
import gr.larryngton.demise.features.modules.impl.player.*;
import gr.larryngton.demise.features.modules.impl.visual.*;
import gr.larryngton.demise.features.values.Value;
import gr.larryngton.demise.utils.animations.Animation;
import gr.larryngton.demise.utils.animations.Direction;
import lombok.Getter;
import net.minecraft.util.Tuple;
import org.lwjglx.input.Keyboard;
import org.lwjglx.input.Mouse;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class ModuleManager {
    private static final Set<Class<? extends Module>> COMBAT_MODULES = Set.of(
            AntiBot.class, Criticals.class, FakeLag.class, KeepSprint.class, KillAura.class, TickBase.class,
            TimerRange.class, Velocity.class, BowAimbot.class, KnockbackDisplacement.class
    );

    private static final Set<Class<? extends Module>> LEGIT_MODULES = Set.of(
            AutoHeal.class, AutoRod.class, AutoWeapon.class, BackTrack.class, CombatHelper.class,
            HitBox.class, JumpReset.class, LegitScaffold.class, NoHitDelay.class, Reach.class,
            SprintReset.class, BlockHit.class, AimAssist.class, SmartClicking.class, AutoClicker.class,
            LagCompAbuse.class, AutoProjectile.class
    );

    private static final Set<Class<? extends Module>> MOVEMENT_MODULES = Set.of(
            AutoWalk.class, Fly.class, Freeze.class, InvMove.class, Jesus.class, JumpDelay.class, LongJump.class,
            MotionModifier.class, NoSlow.class, Phase.class, SafeWalk.class, Sneak.class, Speed.class, Sprint.class,
            Step.class, Strafe.class, TargetStrafe.class, WallClimb.class, NoWeb.class, TestPathFinder.class, FastStop.class,
            PerfectHorseJump.class, Parkour.class, SnapTap.class
    );

    private static final Set<Class<? extends Module>> PLAYER_MODULES = Set.of(
            AntiVoid.class, AutoTool.class, BedBreaker.class, FastBow.class, FastBreak.class, FastPlace.class,
            FastUse.class, Manager.class, NoFall.class, Scaffold.class, Stealer.class, Nuker.class
    );

    private static final Set<Class<? extends Module>> MISC_MODULES = Set.of(
            AutoQueue.class, AutoAccount.class, ExplosionBlock.class, FlagDetector.class, InventorySync.class,
            MurderMystery.class, Test.class, AntiCheat.class, Translator.class, Spammer.class, InputFix.class,
            KillInsults.class, IRC.class, MadeByTheBi11iona1re.class, Debug.class, Opsec.class,
            ViaMCPPlus.class, Annoy.class, PathFinderModule.class, MultiActions.class, BalanceFix.class,
            AFKHandler.class, Dummy.class, Plugins.class, FastFPS.class, AutoMeow.class, RealLag.class,
            Radio.class, ADHD.class, InstaGap.class
    );

    private static final Set<Class<? extends Module>> EXPLOIT_MODULES = Set.of(
            AutoBan.class, Blink.class, ClientSpoofer.class, ComboOneHit.class, Disabler.class, NoGuiClose.class,
            Regen.class, ResetVL.class, Timer.class, PingSpoof.class, InfAura.class, AntiFlag.class, Crasher.class,
            TPAura.class, GodMode.class, BedWalker.class, ChatBypass.class, MessageEncryptor.class, ClickTP.class
    );

    private static final Set<Class<? extends Module>> VISUAL_MODULES = Set.of(
            Atmosphere.class, Animations.class, BlockOverlay.class, Breadcrumbs.class, BreakProgress.class, Cape.class,
            ChestESP.class, ChinaHat.class, ClickGUI.class, CustomSkin.class, CustomWidgetsModule.class, NameTags.class,
            ForceDinnerbone.class, FreeCam.class, FullBright.class, Interface.class, ItemESP.class, ItemPhysics.class,
            MotionBlur.class, HurtCam.class, NoRenderOffsetReset.class, Shaders.class,
            ThirdPersonDistance.class, Trajectories.class, VisualAimPoint.class,
            AFKGui.class, JewESP.class, GlowESP.class, ItemGlow.class, ESP.class, Balance.class,
            HitIndicators.class, FreeLook.class, MentalSupport.class, Crosshair.class,
            NoInterpolation.class, SyncYaw.class, CustomCamera.class, LagDetector.class, SimsESP.class,
            TargetIcon.class, NickHider.class, HitSounds.class
    );

    private final Map<ModuleCategory, List<Module>> modulesByCategory;
    private final List<Module> allModules;
    private final List<Module> allAllModules;
    private final List<Value> allModuleValues;

    public ModuleManager() {
        this.modulesByCategory = new ConcurrentHashMap<>();
        this.allModules = new ArrayList<>();
        this.allModuleValues = new ArrayList<>();
        this.allAllModules = new ArrayList<>();

        initializeModules();
        sortModules();
        registerEventListener();
    }

    private void initializeModules() {
        Arrays.stream(ModuleCategory.values()).forEach(category -> modulesByCategory.put(category, new ArrayList<>()));

        if (!Demise.INSTANCE.isLegit()) {
            initializeCategoryModules(COMBAT_MODULES, ModuleCategory.Combat);
            initializeCategoryModules(LEGIT_MODULES, ModuleCategory.Legit);
            initializeCategoryModules(MOVEMENT_MODULES, ModuleCategory.Movement);
            initializeCategoryModules(PLAYER_MODULES, ModuleCategory.Player);
            initializeCategoryModules(MISC_MODULES, ModuleCategory.Misc);
            initializeCategoryModules(EXPLOIT_MODULES, ModuleCategory.Exploit);
            initializeCategoryModules(VISUAL_MODULES, ModuleCategory.Visual);
        } else {
            initializeCategoryModules(COMBAT_MODULES, ModuleCategory.General);
            initializeCategoryModules(LEGIT_MODULES, ModuleCategory.General);
            initializeCategoryModules(MOVEMENT_MODULES, ModuleCategory.General);
            initializeCategoryModules(PLAYER_MODULES, ModuleCategory.General);
            initializeCategoryModules(MISC_MODULES, ModuleCategory.General);
            initializeCategoryModules(EXPLOIT_MODULES, ModuleCategory.General);
            initializeCategoryModules(VISUAL_MODULES, ModuleCategory.Visual);
        }
    }

    private void initializeCategoryModules(Set<Class<? extends Module>> moduleClasses, ModuleCategory category) {
        moduleClasses.stream()
                .map(this::instantiateModule)
                .filter(Objects::nonNull)
                .forEach(module -> {
                    allAllModules.add(module);

                    if (category == ModuleCategory.Visual || category == ModuleCategory.Legit) {
                        module.setHidden(true);
                    }

                    modulesByCategory.get(category).add(module);
                    allModules.add(module);
                    allModuleValues.addAll(module.getValues());
                });
    }

    private Module instantiateModule(Class<? extends Module> moduleClass) {
        try {
            return moduleClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            Demise.LOGGER.error("Failed to instantiate module: {}", moduleClass.getSimpleName(), e);
            return null;
        }
    }

    private void sortModules() {
        allModules.sort(Comparator.comparing(Module::getName));
        modulesByCategory.values()
                .forEach(modules -> modules.sort(Comparator.comparing(Module::getName)));
    }

    private void registerEventListener() {
        Demise.INSTANCE.getEventManager().register(this);
    }

    public <T extends Module> T getModule(Class<T> moduleClass) {
        if (moduleClass.equals(ClientSettings.class)) {
            throw new IllegalArgumentException("Use Demise.INSTANCE.getClientSettings()");
        }

        return allModules.stream()
                .filter(m -> m.getClass().equals(moduleClass))
                .map(moduleClass::cast)
                .findFirst()
                .orElse(allAllModules.stream()
                        .filter(m -> m.getClass().equals(moduleClass))
                        .map(moduleClass::cast)
                        .findFirst()
                        .orElse(null));
    }

    public Module getModule(String name) {
        return allModules.stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    public List<Module> getModulesByCategory(ModuleCategory category) {
        return Collections.unmodifiableList(modulesByCategory.getOrDefault(category, Collections.emptyList()));
    }

    private final List<Tuple<Integer, Long>> pressedKeys = new ArrayList<>();

    @EventTarget
    public void onKey(KeyPressEvent event) {
        pressedKeys.add(new Tuple<>(event.getKey(), System.currentTimeMillis()));
        allModules.stream().filter(module -> module.getKeyBind() == event.getKey()).forEach(Module::toggle);
    }

    @EventTarget
    public void onGame(GameEvent e) {
        List<Tuple<Integer, Long>> removedKeys = new ArrayList<>();
        pressedKeys.removeIf(key -> {
            if (!isKeyDown(key.getFirst())) {
                removedKeys.add(key);
                return true;
            }
            return false;
        });

        for (Tuple<Integer, Long> list : removedKeys) {
            if (System.currentTimeMillis() - list.getSecond() > 250) {
                allModules.stream().filter(module -> module.getKeyBind() == list.getFirst()).forEach(Module::toggle);
            }
        }
    }

    private boolean isKeyDown(int key) {
        return key != 0 && (key < 0 ? Mouse.isButtonDown(key + 100) : Keyboard.isKeyDown(key));
    }

    public List<Module> getModules() {
        return Collections.unmodifiableList(allModules);
    }

    public List<Module> getEnabledModules() {
        List<Module> enabledModules = new ArrayList<>();
        for (Module module : getModules()) {
            Animation moduleAnimation = module.getAnimation();
            moduleAnimation.setDirection(module.isEnabled() ? Direction.FORWARDS : Direction.BACKWARDS);
            if (!module.isEnabled() && moduleAnimation.finished(Direction.BACKWARDS)) continue;
            enabledModules.add(module);
        }
        return enabledModules;
    }
}