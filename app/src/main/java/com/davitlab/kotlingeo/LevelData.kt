package com.davitlab.kotlingeo

/** Exact world/layout data ported from the original GeoDino buildLevels() definitions. */
data class WorldRect(val x: Float, val y: Float, val w: Float, val h: Float) {
    val right: Float get() = x + w
    val bottom: Float get() = y + h
}

enum class BonusType { COIN, BELL }
data class JumpBox(val rect: WorldRect, val type: BonusType, var hit: Boolean = false)
data class LevelZombieSpawn(val x: Float, val y: Float)
data class LevelSign(val x: Float, val text: String)
data class GeoLevel(
    val name: String,
    val width: Float,
    val spawn: WorldRect,
    val plats: List<WorldRect>,
    val fakeSpikes: List<WorldRect>,
    val spikes: List<WorldRect>,
    val bananas: List<WorldRect>,
    val coins: List<WorldRect>,
    val bells: List<WorldRect>,
    val bushes: List<WorldRect>,
    val signs: List<LevelSign>,
    val flag: WorldRect,
    val zombieSpawns: List<LevelZombieSpawn>,
    val jumpBoxes: List<JumpBox>,
    val bgHue: String
)

object GeoLevels {
    private const val G = GameConfig.GROUND_Y

    val all: List<GeoLevel> = listOf(
        GeoLevel(
            "ეპ.1 — აბსოლუტურად არა კლიკბეიტი", 4400f, WorldRect(60f,G-60f,34f,52f),
            listOf(WorldRect(0f,G,900f,90f),WorldRect(1000f,G,500f,90f),WorldRect(1620f,G,260f,90f),WorldRect(1990f,G-80f,220f,20f),WorldRect(2320f,G,900f,90f),WorldRect(3320f,G-140f,260f,20f),WorldRect(3680f,G,700f,90f)),
            listOf(WorldRect(430f,G-24f,90f,24f)), listOf(WorldRect(920f,G-24f,80f,24f),WorldRect(2870f,G-24f,70f,24f)),
            listOf(WorldRect(1220f,G-14f,40f,14f)),
            (0 until 6).map { WorldRect(1040f+it*60f,G-140f,22f,22f) } + (0 until 5).map { WorldRect(2400f+it*70f,G-120f,22f,22f) },
            listOf(WorldRect(2050f,G-150f,26f,30f),WorldRect(3380f,G-190f,26f,30f)),
            listOf(WorldRect(1750f,G-50f,50f,50f)), listOf(LevelSign(380f,"TOTALLY SAFE PATH >"),LevelSign(1180f,"free ride, no catch"),LevelSign(2800f,"nothing bad here")),
            WorldRect(4260f,G-140f,30f,140f), listOf(LevelZombieSpawn(1200f,G-54f),LevelZombieSpawn(2500f,G-54f),LevelZombieSpawn(3800f,G-54f)),
            listOf(JumpBox(WorldRect(1500f,G-80f,40f,40f),BonusType.COIN),JumpBox(WorldRect(3000f,G-120f,40f,40f),BonusType.BELL)), "blue"
        ),
        GeoLevel(
            "ეპ.2 — ალგორითმის შურისძიება", 4550f, WorldRect(60f,G-60f,34f,52f),
            listOf(WorldRect(0f,G,500f,90f),WorldRect(650f,G-40f,160f,20f),WorldRect(900f,G-120f,160f,20f),WorldRect(1150f,G,260f,90f),WorldRect(1550f,G-60f,180f,20f),WorldRect(1850f,G-160f,180f,20f),WorldRect(2150f,G,700f,90f),WorldRect(2980f,G-40f,200f,20f),WorldRect(3300f,G-140f,200f,20f),WorldRect(3620f,G,900f,90f)),
            listOf(WorldRect(2200f,G-24f,90f,24f),WorldRect(3900f,G-24f,90f,24f)), listOf(WorldRect(560f,G-24f,70f,24f),WorldRect(2500f,G-24f,80f,24f),WorldRect(4200f,G-24f,80f,24f)),
            listOf(WorldRect(1180f,G-14f,40f,14f),WorldRect(3660f,G-14f,40f,14f)),
            (0 until 4).map { WorldRect(660f+it*45f,G-90f,22f,22f) } + (0 until 5).map { WorldRect(2200f+it*70f,G-120f,22f,22f) } + (0 until 4).map { WorldRect(3320f+it*50f,G-190f,22f,22f) },
            listOf(WorldRect(920f,G-170f,26f,30f),WorldRect(1870f,G-210f,26f,30f),WorldRect(3000f,G-90f,26f,30f)),
            listOf(WorldRect(1400f,G-50f,50f,50f),WorldRect(3450f,G-50f,50f,50f)), listOf(LevelSign(600f,"trust me bro"),LevelSign(2160f,"definitely fine"),LevelSign(3920f,"0% chance of prank")),
            WorldRect(4400f,G-140f,30f,140f), listOf(LevelZombieSpawn(800f,G-54f),LevelZombieSpawn(2000f,G-54f),LevelZombieSpawn(3500f,G-54f)),
            listOf(JumpBox(WorldRect(1000f,G-100f,40f,40f),BonusType.COIN),JumpBox(WorldRect(2500f,G-140f,40f,40f),BonusType.BELL)), "pink"
        ),
        GeoLevel(
            "ეპ.3 — მხოლოდ მშვიდი ვაიბები", 4750f, WorldRect(60f,G-60f,34f,52f),
            listOf(WorldRect(0f,G,800f,90f),WorldRect(900f,G-30f,200f,20f),WorldRect(1200f,G,400f,90f),WorldRect(1700f,G-50f,180f,20f),WorldRect(2000f,G,500f,90f),WorldRect(2600f,G-40f,200f,20f),WorldRect(2900f,G,600f,90f),WorldRect(3600f,G-60f,180f,20f),WorldRect(3900f,G,800f,90f)),
            listOf(WorldRect(1500f,G-24f,90f,24f),WorldRect(3200f,G-24f,90f,24f)), listOf(WorldRect(2700f,G-24f,70f,24f)), listOf(WorldRect(2100f,G-14f,40f,14f)),
            (0 until 5).map { WorldRect(920f+it*40f,G-80f,22f,22f) } + (0 until 6).map { WorldRect(2020f+it*60f,G-100f,22f,22f) } + (0 until 5).map { WorldRect(2920f+it*50f,G-110f,22f,22f) },
            listOf(WorldRect(1750f,G-120f,26f,30f),WorldRect(2650f,G-100f,26f,30f),WorldRect(3650f,G-130f,26f,30f)), listOf(WorldRect(2400f,G-50f,50f,50f)),
            listOf(LevelSign(950f,"ez pz"),LevelSign(2050f,"just vibes"),LevelSign(3650f,"almost there")), WorldRect(4600f,G-140f,30f,140f),
            listOf(LevelZombieSpawn(1500f,G-54f),LevelZombieSpawn(3000f,G-54f)), listOf(JumpBox(WorldRect(1300f,G-90f,40f,40f),BonusType.COIN),JumpBox(WorldRect(2800f,G-110f,40f,40f),BonusType.BELL)), "blue"
        ),
        GeoLevel(
            "ეპ.4 — გამოწერების სპეციალური", 4350f, WorldRect(60f,G-60f,34f,52f),
            listOf(WorldRect(0f,G,600f,90f),WorldRect(750f,G-35f,180f,20f),WorldRect(1050f,G,350f,90f),WorldRect(1500f,G-45f,200f,20f),WorldRect(1800f,G,450f,90f),WorldRect(2400f,G-55f,180f,20f),WorldRect(2700f,G,400f,90f),WorldRect(3200f,G-40f,200f,20f),WorldRect(3550f,G,700f,90f)),
            listOf(WorldRect(1900f,G-24f,90f,24f),WorldRect(3400f,G-24f,90f,24f)), listOf(WorldRect(2800f,G-24f,70f,24f)), emptyList(),
            (0 until 6).map { WorldRect(770f+it*30f,G-85f,22f,22f) } + (0 until 7).map { WorldRect(1820f+it*50f,G-105f,22f,22f) } + (0 until 6).map { WorldRect(2720f+it*45f,G-115f,22f,22f) },
            listOf(WorldRect(1100f,G-100f,26f,30f),WorldRect(1550f,G-115f,26f,30f),WorldRect(2450f,G-125f,26f,30f),WorldRect(3250f,G-110f,26f,30f)), emptyList(),
            listOf(LevelSign(800f,"sub pls"),LevelSign(1850f,"bell time")), WorldRect(4200f,G-140f,30f,140f),
            listOf(LevelZombieSpawn(1000f,G-54f),LevelZombieSpawn(2200f,G-54f),LevelZombieSpawn(3400f,G-54f)), listOf(JumpBox(WorldRect(1200f,G-95f,40f,40f),BonusType.COIN),JumpBox(WorldRect(2600f,G-105f,40f,40f),BonusType.BELL)), "pink"
        ),
        GeoLevel(
            "ეპ.5 — მარტივი რეჟიმი ჩართულია", 4800f, WorldRect(60f,G-60f,34f,52f),
            listOf(WorldRect(0f,G,700f,90f),WorldRect(850f,G-25f,200f,20f),WorldRect(1150f,G,500f,90f),WorldRect(1750f,G-35f,180f,20f),WorldRect(2050f,G,600f,90f),WorldRect(2750f,G-30f,200f,20f),WorldRect(3050f,G,550f,90f),WorldRect(3700f,G-40f,180f,20f),WorldRect(4000f,G,750f,90f)),
            listOf(WorldRect(1300f,G-24f,90f,24f),WorldRect(2200f,G-24f,90f,24f),WorldRect(3150f,G-24f,90f,24f)), emptyList(), listOf(WorldRect(2100f,G-14f,40f,14f)),
            (0 until 7).map { WorldRect(870f+it*28f,G-75f,22f,22f) } + (0 until 8).map { WorldRect(2070f+it*55f,G-95f,22f,22f) } + (0 until 7).map { WorldRect(3070f+it*50f,G-105f,22f,22f) },
            listOf(WorldRect(1200f,G-90f,26f,30f),WorldRect(1800f,G-105f,26f,30f),WorldRect(2800f,G-95f,26f,30f),WorldRect(3750f,G-110f,26f,30f)), listOf(WorldRect(2500f,G-50f,50f,50f)),
            listOf(LevelSign(900f,"super easy"),LevelSign(2100f,"no sweat"),LevelSign(3100f,"you got this")), WorldRect(4650f,G-140f,30f,140f),
            listOf(LevelZombieSpawn(1200f,G-54f),LevelZombieSpawn(2800f,G-54f)), listOf(JumpBox(WorldRect(1400f,G-85f,40f,40f),BonusType.COIN),JumpBox(WorldRect(2900f,G-95f,40f,40f),BonusType.BELL)), "blue"
        )
    )
}
