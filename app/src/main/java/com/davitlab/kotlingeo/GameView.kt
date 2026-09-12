package com.davitlab.kotlingeo

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.View
import kotlin.math.*
import kotlin.random.Random

/**
 * Native Kotlin/Android first-pass port of GeoDino.
 *
 * IMPORTANT: this phase intentionally preserves the original 960x540 coordinate
 * system, colors, proportions and visual language. Visual redesign is out of scope.
 */
class GameView(context: Context) : View(context) {
    companion object {
        const val W = 960f
        const val H = 540f
        const val GROUND_Y = 450f
        const val GRAVITY = 0.62f
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private var scale = 1f
    private var offsetX = 0f
    private var offsetY = 0f
    private var lastTime = System.nanoTime()
    private var camX = 0f

    private enum class Mode { LOADING, MENU, PLAYING, PAUSED, GAME_OVER, WIN }
    private var mode = Mode.MENU
    private var time = 0L
    private var score = 0
    private var lives = 3
    private var subs = 0

    private var left = false
    private var right = false
    private var jump = false
    private var jumpWasDown = false

    private data class RectF2(var x: Float, var y: Float, var w: Float, var h: Float) {
        fun right() = x + w
        fun bottom() = y + h
    }
    private data class Coin(var r: RectF2, var taken: Boolean = false)
    private data class Bell(var r: RectF2, var taken: Boolean = false)
    private data class Banana(var r: RectF2, var used: Boolean = false)
    private data class Bush(var r: RectF2, var triggered: Boolean = false)
    private data class Bullet(var x: Float, var y: Float, var vx: Float, var life: Int = 60)
    private data class Particle(var x: Float, var y: Float, var vx: Float, var vy: Float, var life: Float, var color: Int, var size: Float)
    private data class Zombie(
        var x: Float, var y: Float, var w: Float, var h: Float,
        var health: Int, var maxHealth: Int, var speed: Float,
        var color: Int, var size: Float, var vx: Float = 0f, var vy: Float = 0f,
        var facing: Int = 1, var animT: Float = 0f
    )

    private val player = RectF2(60f, GROUND_Y - 60f, 34f, 52f)
    private var playerVx = 0f
    private var playerVy = 0f
    private var playerOnGround = false
    private var playerFacing = 1
    private var playerAnim = 0f
    private var playerInvuln = 0
    private var playerDead = false

    private val plats = mutableListOf<RectF2>()
    private val fakeSpikes = mutableListOf<RectF2>()
    private val spikes = mutableListOf<RectF2>()
    private val bananas = mutableListOf<Banana>()
    private val coins = mutableListOf<Coin>()
    private val bells = mutableListOf<Bell>()
    private val bushes = mutableListOf<Bush>()
    private val signs = mutableListOf<Pair<Float, String>>()
    private val jumpBoxes = mutableListOf<RectF2>()
    private val zombies = mutableListOf<Zombie>()
    private val bullets = mutableListOf<Bullet>()
    private val particles = mutableListOf<Particle>()

    private var levelWidth = 4400f
    private val flag = RectF2(4260f, GROUND_Y - 140f, 30f, 140f)

    init {
        isFocusable = true
        setBackgroundColor(Color.rgb(11, 14, 20))
        buildLevel1()
    }

    private fun buildLevel1() {
        plats.clear(); fakeSpikes.clear(); spikes.clear(); bananas.clear()
        coins.clear(); bells.clear(); bushes.clear(); signs.clear(); jumpBoxes.clear()
        zombies.clear(); bullets.clear(); particles.clear()

        plats += RectF2(0f, GROUND_Y, 900f, 90f)
        plats += RectF2(1000f, GROUND_Y, 500f, 90f)
        plats += RectF2(1620f, GROUND_Y, 260f, 90f)
        plats += RectF2(1990f, GROUND_Y - 80f, 220f, 20f)
        plats += RectF2(2320f, GROUND_Y, 900f, 90f)
        plats += RectF2(3320f, GROUND_Y - 140f, 260f, 20f)
        plats += RectF2(3680f, GROUND_Y, 700f, 90f)

        fakeSpikes += RectF2(430f, GROUND_Y - 24f, 90f, 24f)
        spikes += RectF2(920f, GROUND_Y - 24f, 80f, 24f)
        spikes += RectF2(2870f, GROUND_Y - 24f, 70f, 24f)
        bananas += Banana(RectF2(1220f, GROUND_Y - 14f, 40f, 14f))

        repeat(6) { i -> coins += Coin(RectF2(1040f + i * 60f, GROUND_Y - 140f, 22f, 22f)) }
        repeat(5) { i -> coins += Coin(RectF2(2400f + i * 70f, GROUND_Y - 120f, 22f, 22f)) }
        bells += Bell(RectF2(2050f, GROUND_Y - 150f, 26f, 30f))
        bells += Bell(RectF2(3380f, GROUND_Y - 190f, 26f, 30f))
        bushes += Bush(RectF2(1750f, GROUND_Y - 50f, 50f, 50f))
        signs += 380f to "TOTALLY SAFE PATH >"
        signs += 1180f to "free ride, no catch"
        signs += 2800f to "nothing bad here"
        jumpBoxes += RectF2(1500f, GROUND_Y - 80f, 40f, 40f)
        jumpBoxes += RectF2(3000f, GROUND_Y - 120f, 40f, 40f)

        spawnZombie(1200f, GROUND_Y - 54f)
        spawnZombie(2500f, GROUND_Y - 54f)
        spawnZombie(3800f, GROUND_Y - 54f)
        resetPlayer()
        camX = 0f
    }

    private fun resetPlayer() {
        player.x = 60f; player.y = GROUND_Y - 60f
        playerVx = 0f; playerVy = 0f; playerOnGround = false
        playerFacing = 1; playerInvuln = 90; playerDead = false
    }

    private fun startGame() {
        score = 0; lives = 3; subs = 0; buildLevel1(); mode = Mode.PLAYING
    }

    private fun restartLevel() {
        if (mode == Mode.PLAYING || mode == Mode.PAUSED) {
            lives = 3; score = 0; subs = 0; buildLevel1(); mode = Mode.PLAYING
        }
    }

    private fun spawnZombie(x: Float, y: Float) {
        val type = Random.nextFloat()
        var health = 2; var speed = 1.5f; var color = Color.rgb(74, 139, 74); var size = 1f
        if (type < .3f) { health = 1; speed = 2.2f; color = Color.rgb(106, 155, 106); size = .9f }
        else if (type >= .7f) { health = 4; speed = .8f; color = Color.rgb(58, 123, 58); size = 1.2f }
        zombies += Zombie(x, y, 36f * size, 54f * size, health, health, speed, color, size)
    }

    private fun shoot() {
        if (mode != Mode.PLAYING || playerDead) return
        bullets += Bullet(player.x + player.w / 2f, player.y + player.h / 2f, playerFacing * 12f)
        repeat(5) { particles += Particle(player.x + player.w / 2f, player.y + player.h / 2f, (Random.nextFloat() - .5f) * 8f, (Random.nextFloat() - .5f) * 8f - 3f, 50f, Color.rgb(255,210,63), 3f + Random.nextFloat() * 4f) }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        scale = min(width / W, height / H)
        offsetX = (width - W * scale) / 2f
        offsetY = (height - H * scale) / 2f
        canvas.save()
        canvas.translate(offsetX, offsetY)
        canvas.scale(scale, scale)

        val now = System.nanoTime()
        val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0f, .033f)
        lastTime = now
        time += (dt * 1000f).toLong()

        if (mode == Mode.PLAYING) update(dt)
        drawBackground(if (false) "pink" else "blue")
        drawWorld(canvas)
        drawHud(canvas)
        if (mode == Mode.MENU) drawMenu(canvas)
        if (mode == Mode.PAUSED) drawPause(canvas)
        if (mode == Mode.GAME_OVER) drawOverlay(canvas, "GAME OVER")
        if (mode == Mode.WIN) drawOverlay(canvas, "WIN!")

        canvas.restore()
        postInvalidateOnAnimation()
    }

    private fun update(dt: Float) {
        if (playerInvuln > 0) playerInvuln--
        playerAnim += dt * 1000f
        val target = when {
            left && !right -> -3.4f
            right && !left -> 3.4f
            else -> 0f
        }
        playerVx += (target - playerVx) * .22f
        if (abs(playerVx) > .1f) playerFacing = if (playerVx > 0) 1 else -1

        if (jump && !jumpWasDown && playerOnGround) {
            playerVy = -11.5f
            playerOnGround = false
        }
        jumpWasDown = jump

        playerVy = (playerVy + GRAVITY).coerceAtMost(18f)
        player.x += playerVx
        player.y += playerVy

        playerOnGround = false
        for (p in plats) {
            if (overlap(player, p) && playerVy >= 0f) {
                val previousBottom = player.y + player.h - playerVy
                if (previousBottom <= p.y + 3f) {
                    player.y = p.y - player.h
                    playerVy = 0f
                    playerOnGround = true
                }
            }
        }
        player.x = player.x.coerceIn(0f, levelWidth - player.w)
        if (player.y > H + 120f) hurt("გზიდან გადმოვარდი!")

        for (s in spikes) if (overlap(player, s)) hurt("ეკლებმა დაგიჭირეს!")
        for (b in bananas) if (!b.used && overlap(player, b.r)) { b.used = true; playerVx *= -1.8f; playerVy = -4f }
        for (f in fakeSpikes) if (overlap(player, f)) { /* prank visual state is added in next parity pass */ }
        for (c in coins) if (!c.taken && overlap(player, c.r)) { c.taken = true; score += 10 }
        for (b in bells) if (!b.taken && overlap(player, b.r)) { b.taken = true; subs = (subs + 20).coerceAtMost(100) }

        updateZombies(dt)
        updateBullets()
        updateParticles()

        camX += ((player.x - W * .35f) - camX) * .12f
        camX = camX.coerceIn(0f, max(0f, levelWidth - W))

        if (overlap(player, flag)) mode = Mode.WIN
    }

    private fun updateZombies(dt: Float) {
        for (i in zombies.indices.reversed()) {
            val z = zombies[i]
            z.animT += dt * 1000f
            var platform: RectF2? = null
            for (p in plats) {
                if (overlap(zRect(z), p) && z.vy >= 0f) {
                    val previousBottom = z.y + z.h - z.vy
                    if (previousBottom <= p.y + 2f) {
                        z.y = p.y - z.h; z.vy = 0f; platform = p
                    }
                }
            }
            val dx = player.x - z.x
            val dy = player.y - z.y
            val dist = sqrt(dx * dx + dy * dy)
            if (platform != null && dist < 500f && dist > 30f) {
                val dir = if (dx > 0) 1 else -1
                val nextX = z.x + dir * 20f
                val safe = nextX >= platform.x && nextX + z.w <= platform.right()
                if (safe) { z.vx = (dx / dist) * z.speed; z.facing = dir } else z.vx *= .8f
            } else z.vx *= .9f
            z.x += z.vx
            z.vy = (z.vy + GRAVITY).coerceAtMost(18f)
            z.y += z.vy
            if (z.y > H + 200f) zombies.removeAt(i)
            else if (overlap(player, zRect(z)) && playerInvuln <= 0) hurt("ზომბიმ დაგიჭირა!")
            if (z.health <= 0) { score += z.maxHealth * 15; zombies.removeAt(i) }
        }
    }

    private fun updateBullets() {
        for (i in bullets.indices.reversed()) {
            val b = bullets[i]
            b.x += b.vx; b.life--
            var hit = false
            for (z in zombies) {
                if (overlap(RectF2(b.x - 4f, b.y - 4f, 8f, 8f), zRect(z))) { z.health--; hit = true; break }
            }
            if (hit || b.life <= 0 || b.x < camX - 50f || b.x > camX + W + 50f) bullets.removeAt(i)
        }
    }

    private fun updateParticles() {
        for (i in particles.indices.reversed()) {
            val p = particles[i]
            p.x += p.vx; p.y += p.vy; p.vy += .3f; p.life--; p.size *= .95f
            if (p.life <= 0 || p.size < .5f) particles.removeAt(i)
        }
    }

    private fun hurt(message: String) {
        if (playerInvuln > 0 || playerDead) return
        lives--
        playerInvuln = 90
        if (lives <= 0) { playerDead = true; mode = Mode.GAME_OVER }
        else resetPlayer()
    }

    private fun drawBackground(hue: String) {
        val skyTop = if (hue == "pink") Color.rgb(42,16,48) else Color.rgb(13,27,51)
        val skyBot = if (hue == "pink") Color.rgb(90,20,64) else Color.rgb(18,58,94)
        val shader = LinearGradient(0f, 0f, 0f, H, skyTop, skyBot, Shader.TileMode.CLAMP)
        paint.shader = shader
        paint.style = Paint.Style.FILL
        canvasRef!!.drawRect(0f, 0f, W, H, paint)
        paint.shader = null

        val hillColor = if (hue == "pink") Color.argb(38,255,61,110) else Color.argb(38,56,198,255)
        paint.color = hillColor
        for (layer in 0..2) {
            val speed = .2f + layer * .1f
            for (i in -1..5) {
                val bx = ((i * 300f - camX * speed) % 1800f + 1800f) % 1800f
                canvasRef!!.drawOval(bx + 150f - 220f + layer * 30f, GROUND_Y + 30f + layer * 20f - 90f + layer * 15f, bx + 150f + 220f - layer * 30f, GROUND_Y + 30f + layer * 20f, paint)
            }
        }

        paint.color = Color.argb(153,255,255,255)
        for (i in 0 until 50) {
            val sx = ((i * 137f - camX * .15f) % W + W) % W
            val sy = (i * 53f) % (GROUND_Y - 40f)
            paint.alpha = ((sin(time * .003 + i) * .3 + .7) * 255).toInt().coerceIn(0,255)
            canvasRef!!.drawRect(sx, sy + 10f, sx + 2f, sy + 12f, paint)
        }
        paint.alpha = 255
    }

    private var canvasRef: Canvas? = null

    private fun drawWorld(canvas: Canvas) {
        canvasRef = canvas
        drawGround(canvas)
        for (s in signs) drawSign(canvas, s.first, s.second)
        for (f in fakeSpikes) drawFakeSpike(canvas, f)
        for (s in spikes) drawSpike(canvas, s)
        for (b in bananas) drawBanana(canvas, b)
        for (c in coins) drawCoin(canvas, c)
        for (b in bells) drawBell(canvas, b)
        for (b in bushes) drawBush(canvas, b)
        drawFlag(canvas, flag)
        for (z in zombies) drawZombie(canvas, z)
        for (b in bullets) drawBullet(canvas, b)
        drawParticles(canvas)
        drawPlayer(canvas)
        canvasRef = null
    }

    private fun visible(r: RectF2): Boolean = r.right() - camX >= -100f && r.x - camX <= W + 100f

    private fun drawGround(canvas: Canvas) {
        for (p in plats) {
            if (!visible(p)) continue
            val x = p.x - camX
            val shader = LinearGradient(0f, p.y, 0f, p.bottom(), Color.rgb(79,214,123), Color.rgb(83,60,39), Shader.TileMode.CLAMP)
            paint.shader = shader; paint.style = Paint.Style.FILL
            canvas.drawRect(x, p.y, x + p.w, p.bottom(), paint)
            paint.shader = null; paint.color = Color.rgb(95,214,139)
            for (i in 0 until p.w.toInt() step 20) {
                if ((i + camX.toInt()) % 30 == 0) {
                    path.reset(); path.moveTo(x+i,p.y); path.lineTo(x+i+5,p.y-8); path.lineTo(x+i+10,p.y); path.close(); canvas.drawPath(path,paint)
                }
            }
            paint.color = Color.argb(64,0,0,0); paint.style = Paint.Style.STROKE; canvas.drawRect(x,p.y,x+p.w,p.bottom(),paint); paint.style = Paint.Style.FILL
        }
    }

    private fun drawSign(canvas: Canvas, sxWorld: Float, text: String) {
        val sx = sxWorld - camX; if (sx < -150 || sx > W + 50) return
        val y = GROUND_Y - 70f
        paint.color = Color.rgb(138,90,42); canvas.drawRect(sx+18,y+20,sx+26,y+70,paint)
        paint.color = Color.rgb(232,199,122); canvas.drawRect(sx,y,sx+60,y+26,paint)
        paint.color = Color.rgb(92,59,26); paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f; canvas.drawRect(sx,y,sx+60,y+26,paint); paint.style=Paint.Style.FILL
        paint.color = Color.rgb(42,26,8); paint.textSize=9f; paint.textAlign=Paint.Align.CENTER
        canvas.drawText(text.take(12),sx+30,y+16,paint)
    }

    private fun drawFakeSpike(canvas: Canvas, f: RectF2) {
        val sx=f.x-camX; if(!visible(f)) return
        paint.color=Color.rgb(216,216,224)
        repeat(4){i-> path.reset(); path.moveTo(sx+i*f.w/4,f.y+f.h); path.lineTo(sx+i*f.w/4+f.w/8,f.y); path.lineTo(sx+(i+1)*f.w/4,f.y+f.h); path.close(); canvas.drawPath(path,paint)}
    }

    private fun drawSpike(canvas: Canvas, s: RectF2) {
        val sx=s.x-camX; if(!visible(s)) return
        paint.color=Color.rgb(255,61,110)
        repeat(4){i-> path.reset(); path.moveTo(sx+i*s.w/4,s.y+s.h); path.lineTo(sx+i*s.w/4+s.w/8,s.y-6); path.lineTo(sx+(i+1)*s.w/4,s.y+s.h); path.close(); canvas.drawPath(path,paint)}
    }

    private fun drawBanana(canvas: Canvas,b: Banana){ if(b.used||!visible(b.r))return; val sx=b.r.x-camX; paint.color=Color.rgb(255,210,63); canvas.drawOval(sx,b.r.y,sx+b.r.w,b.r.y+b.r.h,paint) }

    private fun drawCoin(canvas: Canvas,c: Coin){
        if(c.taken||!visible(c.r))return; val sx=c.r.x-camX; val bob=sin(time*.006+c.r.x)*3f
        paint.color=Color.argb(76,255,210,63); canvas.drawCircle(sx+c.r.w/2,c.r.y+c.r.h/2+bob,c.r.w/2+4,paint)
        paint.color=Color.rgb(255,210,63); canvas.drawCircle(sx+c.r.w/2,c.r.y+c.r.h/2+bob,c.r.w/2,paint)
        paint.color=Color.rgb(107,74,18); paint.textSize=12f; paint.textAlign=Paint.Align.CENTER; canvas.drawText("👍",sx+c.r.w/2,c.r.y+c.r.h/2+4+bob,paint)
    }

    private fun drawBell(canvas: Canvas,b: Bell){
        if(b.taken||!visible(b.r))return; val sx=b.r.x-camX; val bob=sin(time*.005+b.r.x)*4f
        paint.color=Color.argb(76,255,61,110); canvas.drawCircle(sx+b.r.w/2,b.r.y+b.r.h/2+bob,18f,paint)
        paint.color=Color.rgb(255,255,255); paint.textSize=24f; paint.textAlign=Paint.Align.CENTER; canvas.drawText("🔔",sx+b.r.w/2,b.r.y+b.r.h+bob,paint)
    }

    private fun drawBush(canvas: Canvas,b: Bush){
        if(!visible(b.r))return; val sx=b.r.x-camX; paint.color=if(b.triggered)Color.rgb(47,106,47)else Color.rgb(36,90,36)
        canvas.drawCircle(sx+b.r.w*.3f,b.r.y+b.r.h*.6f,b.r.w*.35f,paint); canvas.drawCircle(sx+b.r.w*.6f,b.r.y+b.r.h*.5f,b.r.w*.4f,paint); canvas.drawCircle(sx+b.r.w*.85f,b.r.y+b.r.h*.65f,b.r.w*.3f,paint)
    }

    private fun drawFlag(canvas: Canvas,f:RectF2){ if(!visible(f))return; val sx=f.x-camX; paint.color=Color.rgb(207,207,207); canvas.drawRect(sx+f.w/2-2,f.y,sx+f.w/2+2,f.y+f.h,paint); paint.color=Color.rgb(255,61,110); path.reset(); path.moveTo(sx+f.w/2+2,f.y+6); path.lineTo(sx+f.w/2+46,f.y+16); path.lineTo(sx+f.w/2+2,f.y+30); path.close(); canvas.drawPath(path,paint); paint.color=Color.WHITE; paint.textSize=10f; paint.textAlign=Paint.Align.CENTER; canvas.drawText("გამოწერე",sx+f.w/2+24,f.y+19,paint) }

    private fun drawZombie(canvas:Canvas,z:Zombie){
        val sx=z.x-camX;if(sx+z.w<0||sx>W)return;canvas.save();canvas.translate(sx+z.w/2,z.y+z.h/2);canvas.scale(z.facing*z.size,z.size)
        val wobble=sin(z.animT*.03f)*2f;paint.color=z.color;canvas.drawRect(-12,z.h*.1f+wobble,12,z.h*.1f+wobble+30,paint);canvas.drawOval(-14,-25,14,5,paint)
        paint.color=Color.RED;canvas.drawCircle(-5,-12,3,paint);canvas.drawCircle(5,-12,3,paint);paint.color=Color.rgb(42,74,42);canvas.drawOval(-6,-6,6,2,paint)
        paint.color=z.color;canvas.drawRect(-18,z.h*.1f+wobble,-12,z.h*.1f+wobble+20,paint);canvas.drawRect(12,z.h*.1f+wobble,18,z.h*.1f+wobble+20,paint)
        paint.color=Color.rgb(58,123,58);val lw=sin(z.animT*.04f)*3f;canvas.drawRect(-10,z.h*.35f+wobble+lw,-2,z.h*.35f+wobble+lw+18,paint);canvas.drawRect(2,z.h*.35f+wobble-lw,10,z.h*.35f+wobble-lw+18,paint)
        paint.color=Color.argb(128,0,0,0);canvas.drawRect(-15,-35,15,-29,paint);paint.color=Color.rgb(255,61,110);canvas.drawRect(-15,-35,30*(z.health.toFloat()/z.maxHealth)-15,-29,paint);canvas.restore()
    }

    private fun drawBullet(canvas:Canvas,b:Bullet){val sx=b.x-camX;if(sx<0||sx>W)return;paint.color=Color.rgb(255,210,63);canvas.drawCircle(sx,b.y,5f,paint);paint.color=Color.argb(128,255,210,63);canvas.drawCircle(sx-b.vx*2,b.y,3f,paint)}

    private fun drawParticles(canvas:Canvas){for(p in particles){val sx=p.x-camX;if(sx<-50||sx>W+50)continue;paint.alpha=(p.life/60f*255).toInt().coerceIn(0,255);paint.color=p.color;canvas.drawCircle(sx,p.y,p.size,paint)}paint.alpha=255}

    private fun drawPlayer(canvas:Canvas){
        val sx=player.x-camX;val sy=player.y;canvas.save();canvas.translate(sx+player.w/2,sy+player.h/2);canvas.scale(playerFacing.toFloat(),1f)
        if(playerInvuln>0 && playerInvuln/4%2==0)paint.alpha=102
        val running=abs(playerVx)>.5f&&playerOnGround;val runCycle=if(running)sin(playerAnim*.02f)*10f else 0f
        paint.color=Color.rgb(139,92,246);canvas.drawRect(-10,player.h*.18f+runCycle,-2,player.h*.18f+runCycle+22,paint);canvas.drawRect(2,player.h*.18f-runCycle,10,player.h*.18f-runCycle+22,paint)
        paint.color=Color.rgb(56,198,255);canvas.drawRect(-13,-12,13,16,paint);paint.color=Color.rgb(20,24,38);canvas.drawRect(-11,-25,11,-8,paint)
        paint.color=Color.WHITE;canvas.drawCircle(-5,-17,2.5f,paint);canvas.drawCircle(5,-17,2.5f,paint)
        paint.color=Color.rgb(56,198,255);canvas.drawRect(-16,0,-10,20,paint);canvas.drawRect(10,0,16,20,paint)
        paint.alpha=255;canvas.restore()
    }

    private fun drawHud(canvas:Canvas){
        paint.color=Color.WHITE;paint.textSize=16f;paint.textAlign=Paint.Align.LEFT
        canvas.drawText("❤ $lives",18f,28f,paint);canvas.drawText("Score: $score",18f,50f,paint);canvas.drawText("🔔 $subs%",18f,72f,paint)
        paint.textAlign=Paint.Align.RIGHT;canvas.drawText("K = სროლა",W-18f,28f,paint);paint.textAlign=Paint.Align.LEFT
    }

    private fun drawMenu(canvas:Canvas){paint.color=Color.argb(190,11,14,20);canvas.drawRect(0f,0f,W,H,paint);paint.color=Color.WHITE;paint.textAlign=Paint.Align.CENTER;paint.textSize=34f;canvas.drawText("გეოდინო: ხულიგანური პარკური",W/2,190f,paint);paint.textSize=18f;canvas.drawText("SPACE / ENTER — დაწყება",W/2,245f,paint);canvas.drawText("ისრები / A D გადაადგილება • SPACE / W ხტომა • P პაუზა • R თავიდან დაწყება • K სროლა",W/2,285f,paint)}
    private fun drawPause(canvas:Canvas){paint.color=Color.argb(150,11,14,20);canvas.drawRect(0f,0f,W,H,paint);paint.color=Color.WHITE;paint.textAlign=Paint.Align.CENTER;paint.textSize=42f;canvas.drawText("PAUSED",W/2,H/2,paint)}
    private fun drawOverlay(canvas:Canvas,text:String){paint.color=Color.argb(190,11,14,20);canvas.drawRect(0f,0f,W,H,paint);paint.color=Color.WHITE;paint.textAlign=Paint.Align.CENTER;paint.textSize=48f;canvas.drawText(text,W/2,220f,paint);paint.textSize=18f;canvas.drawText("SPACE / ENTER — თავიდან",W/2,270f,paint)}

    private fun overlap(a:RectF2,b:RectF2)=a.x<b.right()&&a.right()>b.x&&a.y<b.bottom()&&a.bottom()>b.y
    private fun zRect(z:Zombie)=RectF2(z.x,z.y,z.w,z.h)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x=(event.x-offsetX)/scale;val y=(event.y-offsetY)/scale
        when(event.actionMasked){
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if(mode==Mode.MENU||mode==Mode.GAME_OVER||mode==Mode.WIN){startGame();return true}
                if(mode==Mode.PAUSED){mode=Mode.PLAYING;return true}
                if(y>H-110){when{ x<220->left=true; x<450->right=true; x>700->jump=true }}
                else if(x>W*.55f) shoot()
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {left=false;right=false;jump=false}
        }
        return true
    }
}
