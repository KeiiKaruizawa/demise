package gr.larryngton.demise.features.modules.impl.misc;

import gr.larryngton.demise.Demise;
import gr.larryngton.demise.events.annotations.EventTarget;
import gr.larryngton.demise.events.impl.player.KillEvent;
import gr.larryngton.demise.features.modules.Module;
import gr.larryngton.demise.features.modules.ModuleInfo;
import gr.larryngton.demise.features.values.impl.BoolValue;
import gr.larryngton.demise.features.values.impl.ModeValue;
import gr.larryngton.demise.utils.math.MathUtils;
import gr.larryngton.demise.utils.misc.ChatUtils;

@ModuleInfo(name = "KillInsults", description = "Insults people after you kill them.")
public class KillInsults extends Module {
    private final ModeValue mode = new ModeValue("Mode", new String[]{"demise", "Rise 5", "Sigma"}, "demise", this);
    private final BoolValue debug = new BoolValue("Debug", false, this);

    @EventTarget
    public void onKill(KillEvent e) {
        String insult = switch (mode.get()) {
            case "Rise 5" -> insults[MathUtils.randomizeInt(0, insults.length)];
            case "Sigma" -> sigma[MathUtils.randomizeInt(0, sigma.length)];
            default -> demise[MathUtils.randomizeInt(0, demise.length)];
        };

        insult = insult.replaceAll(":target:", e.getTarget().getName());

        if (!debug.get()) {
            ChatUtils.sendMessageServer(insult);
        } else {
            ChatUtils.sendMessageClient(insult);
        }
    }

    private final String[] demise = {
            "imagine folding to a polar bear",
            "is :target: using LiquidBounce? I can tell",
            "rise is unironically better than whatever :target: is on",
            "POV: augustus",
            "i am using noise9999 rotations",
            "pay to lose",
            "i don't cheat, :target: just needs to aim better",
            "poolar anticheat",
            ":target: got damage mitigated btw",
            "the problem :target:'s having looks like a skin color issue",
            "ez",
            "Yae Miko is larryngton's wife",
            "no hax just beta testing the anticheat with demise",
            "chatgpt pro billionaire bypass india vs pakistan",
            "Math.random() bypass",
            "people like :target: were the inspiration for abortion",
            ":target: should have been used as an example for a condom campaign",
            "jump off a bridge with a rope tied around your neck :target:",
            "hacked by eonian :joy:",
            "foid annihilated",
            ":target: might have been an idf soldier",
            "1 + Math.random() / 10000000 gcd bypass"
    };

    private final String[] insults = {
            "Did :target: pay for that loss?",
            "Did :target:'s dad not come back after he wanted to buy some milk?",
            "Are you afraid of me",
            "Why not use demise?",
            ":target: I saw you in femboy meetup, hi!",
            "Get demise, don't be like :target:",
            "Did :target: forget to left click?",
            ":target: takes up 2 seats on the bus",
            "No demise?",
            "It is impossible to miss :target: with their man boobs",
            "Come on :target:, report me to the obese staff",
            ":target: is the type to overdose on Benadryl for a Tiktok video",
            "No wonder :target: dropped out of college",
            "Here's your ticket to spectator",
            ":target: said they would never give me up and never let me down, I am sad",
            "The latest update to demise fps booster client gave me 1000 fps and regedits for better velocity",
            ":target: became transgender just to join the 50% a day later",
            "Drink hand sanitizer so we can get rid of :target:",
            "Even the MC Virgins are less virgin than :target:",
            ":target:'s free trial of life has expired",
            ":target: is socially awkward",
            "I bet :target: believes in the flat earth",
            ":target: is the reason why society is failing",
            "Pay to lose",
            "Why would I be cheating when I am recording?",
            ":target: is such a that degenerate :target: believes EQ has more value than IQ",
            "The air could've took :target: away because of how weak :target: is",
            "Even Kurt Cobain is more alive than :target: with his wounds from a shotgun and heroin in his veins",
            ":target: is breaking down more than Nirvana after Kurt Cobain's death",
            "Does :target: buy their groceries at the dollar store?",
            "Does :target: need some pvp advice?",
            "I'd smack :target:, but that would be animal abuse",
            "I don't cheat, :target: just needs to click faster",
            "Welcome to my rape dungeon! population: :target:!",
            ":target: pressed the wrong button when they installed Minecraft?",
            "If the body is 70% water than how is :target:'s body 100% salt?",
            "demise " + Demise.INSTANCE.version + " is sexier than :target:",
            "Oh, :target: is recording? Well I am too",
            ":target: is the type of person who would brute force interpolation",
            ":target: go drown in your own salt",
            ":target: is literally heavier than Overflow",
            "Excuse me :target:, I don't speak retard",
            "Hmm, the problem :target:'s having looks like a skin color issue",
            ":target: I swear I'm on Lunar Client",
            "Hey! Wise up :target:! Don't waste your time without demise",
            ":target: didn't even stand a chance",
            "If opposites attract I hope :target: finds someone who is intelligent, honest and cultured",
            "If laughter is the best medicine, :target:'s face must be curing the world",
            ":target: is the type of person to climb over a glass wall to see what's on the other side",
            "What does :target:'s IQ and their girlfriend have in common? They're both below 5."
    };

    // im aware that demise means death btw
    private final String[] sigma = {
            "Download demise to kick ass while listening to some badass music!",
            "Why demise? Cause it is the addition of pure skill and incredible intellectual abilities",
            "You have been oofed by demise oof oof",
            "I am not racist, but I only like demise users. so git gud noobs",
            "Quick Quiz: I am larryngton's son, who am I? demise",
            "Wow! My combo is demise'n!",
            "What should I choose? demise or demise?",
            "Get demise Client, its free!",
            "Bigmama and demisemama",
            "I don't hack I just demise",
            "demise client is your new home",
            "Look a divinity! He definitely must use demise!",
            "In need of a cute present for Christmas? demise is all you need!",
            "I have a good demise config, don't blame me",
            "Don't piss me off or you will discover the true power of demise's inf reach",
            "demise never dies",
            "demise will help you! Oops, i killed you instead.",
            "#NoHaxJustdemise",
            "Order free baguettes with demise client",
            "Another demise user? Awww man",
            "demise utility client no hax 100%",
            "Hypixel wants to know demise owner's location [Accept] [Deny]",
            "I am a demise-magician, thats how I am able to do all those block game tricks",
            "Stop it, get some help! Get demise",
            "demise users belike: Hit or miss I guess I never miss!",
            "I dont hack i just have demise Gaming Chair",
            "Stop Hackustation me cuz im just demise",
            "Imagine using anything but demise",
            "No hax just beta testing the anti-cheat with demise",
            "Don't forget to report me for demise on the forums!",
            "don't use demise? ok boomer",
            "demise is better than Optifine",
            "It's not BlockFly it's Scaffold in demise!",
            "How come a noob like you not use demise?",
            "A mother becomes a true grandmother the day she gets demise " + Demise.INSTANCE.version,
            "Fly faster than light, only available in demise™",
            "Behind every demise user, is an incredibly cool human being. Trust me, cooler than you.",
            "Hello demise my old friend...",
            "#SwitchTodemise",
            "What? You've never downloaded demise? You know it's the best right?",
            "Your client sucks, just get demise",
            "demise made this world a better place, killing you with it even more"
    };
}
