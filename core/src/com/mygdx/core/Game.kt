package com.mygdx.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.FloatAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Intersector
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import java.util.*

val levels = arrayOf(
        "shift.txt",
        "tight.txt",
        "stairs.txt",
        "labyrinth.txt",

        "separatism.txt",
        "small drop.txt",
        "dropfun.txt",

        "sokoban.txt",
        "four boxes.txt",
        "end.txt"
)
var currentLevel = 0
val cameraFollowsPlayer = true
val yMin = -50f // y limit for falling stuff
val cameraOffset = Vector3(5f, 10f, 5f)

val modelBuilder = ModelBuilder()
val font = BitmapFont()
val layout = GlyphLayout()

val disposables = mutableListOf<com.badlogic.gdx.utils.Disposable>()

var precision = true
val precisionButton = UIButton("Turn precision OFF", 0f, 700f, layout, font, ::precision)
fun precision(button: UIButton) {
    precision = !precision
    if (precision) {
        button.text = "Turn precision OFF"
    } else {
        button.text = "Turn precision ON"
    }
}

var setPlayerOrigin = false
val setPlayerOriginButton = UIButton("Set player", 0f, 670f, layout, font, ::setPlayerOrigin)
fun setPlayerOrigin(button: UIButton) {
    setPlayerOrigin = true
}

var setObjective = false
val setObjectiveButton = UIButton("Set objective", 0f, 640f, layout, font, ::setObjective)
fun setObjective(button: UIButton) {
    setObjective = true
}

var addBox = false
val addBoxButton = UIButton("Add box", 0f, 610f, layout, font, ::addBox)
fun addBox(button: UIButton) {
    addBox = true
}

val editorButtons = arrayOf(
        precisionButton,
        setPlayerOriginButton,
        setObjectiveButton,
        addBoxButton)


val playerOrigin = Vector3()
val objectiveOrigin = Vector3()

class Game {
    enum class GameState {
        Normal, LevelTransition, End, Editor,
    }

    var state = GameState.Normal

    val viewport: IntRect
    val spriteBatch = SpriteBatch()
    val modelBatch = ModelBatch()
    val environment = Environment()
    val camera3D = PerspectiveCamera(67f, Constants.VIEWPORT_WIDTH.toFloat(), Constants.VIEWPORT_HEIGHT.toFloat())
    val camController = CameraInputController(camera3D)

    val end = assets.getTexture("end.png")

    val player: Player
    val playerMirror: Player
    var objective = Voxel(0, 0, 100, VoxelType.Objective)
    var objectiveMirror = Voxel(0, 0, -100, VoxelType.Objective)
    val mirror: ModelInstance
    val objectiveVoxel: ModelInstance
    val groundVoxel: ModelInstance
    val boxVoxel: ModelInstance
    val touchedGroundVoxel: ModelInstance

    val ambientColor = Color(0.3f, 0.3f, 0.31f, 1f)
    val directionalColor = Color(0.85f, 0.85f, 0.9f, 1f)

    val preferences = Gdx.app.getPreferences("preferences")

    init {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Texture.setAssetManager(assets)

        currentLevel = preferences.getInteger("currentLevel", 0)

        val width = Gdx.graphics.width
        val height = Gdx.graphics.height
        val aspectRatio = width.toFloat() / height.toFloat()
        val scale: Float
        val crop = Vector2()
        if (aspectRatio > Constants.ASPECT_RATIO) {
            scale = height.toFloat() / Constants.VIEWPORT_HEIGHT.toFloat()
            crop.x = (width - Constants.VIEWPORT_WIDTH * scale) / 2f
        } else if (aspectRatio < Constants.ASPECT_RATIO) {
            scale = width.toFloat() / Constants.VIEWPORT_WIDTH.toFloat()
            crop.y = (height - Constants.VIEWPORT_HEIGHT * scale) / 2f;
        } else {
            scale = width.toFloat() / Constants.VIEWPORT_WIDTH.toFloat()
        }
        val w = Constants.VIEWPORT_WIDTH * scale
        val h = Constants.VIEWPORT_HEIGHT * scale
        viewport = IntRect(crop.x.toInt(), crop.y.toInt(), w.toInt(), h.toInt())
        spriteBatch.projectionMatrix.setToOrtho2D(0f, 0f, viewport.w.toFloat(), viewport.h.toFloat())

        environment.set(ColorAttribute(ColorAttribute.AmbientLight, ambientColor))
        environment.set(ColorAttribute(ColorAttribute.Fog, Color.SLATE))
        environment.add(DirectionalLight().set(directionalColor.r, directionalColor.g, directionalColor.b, -1f, -0.8f, -0.2f))

        val mirrorReferenceModel = assets.getModel("mirror.g3db")
        val mirrorModel = modelBuilder.createBox(40f, 40f, 0.1f,
                mirrorReferenceModel.nodes[0].parts[0].material.copy(),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        disposables.add(mirrorModel)
        mirror = ModelInstance(mirrorModel, 0f, 0f, -0.5f)

        val groundModel = modelBuilder.createBox(0.9f, 0.9f, 0.9f,
                Material(ColorAttribute.createDiffuse(Color.WHITE),
                        ColorAttribute.createSpecular(1f, 1f, 1f, 1f),
                        FloatAttribute.createShininess(64f)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        disposables.add(groundModel)
        groundVoxel = ModelInstance(groundModel)

        val touchedGroundModel = modelBuilder.createBox(0.9f, 0.9f, 0.9f,
                Material(ColorAttribute.createDiffuse(Color.LIGHT_GRAY),
                        ColorAttribute.createSpecular(1f, 1f, 1f, 1f),
                        FloatAttribute.createShininess(64f)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        disposables.add(touchedGroundModel)
        touchedGroundVoxel = ModelInstance(touchedGroundModel)

        val objectiveModel = modelBuilder.createBox(0.9f, 0.9f, 0.9f,
                Material(ColorAttribute.createDiffuse(Color.GOLD),
                        ColorAttribute.createSpecular(1f, 1f, 1f, 0.5f),
                        FloatAttribute.createShininess(64f)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        disposables.add(objectiveModel)
        objectiveVoxel = ModelInstance(objectiveModel)

        val boxModel = modelBuilder.createBox(0.9f, 0.9f, 0.9f,
                Material(ColorAttribute.createDiffuse(Color.BROWN),
                        ColorAttribute.createSpecular(1f, 1f, 1f, 0.5f),
                        FloatAttribute.createShininess(64f)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal).toLong())
        disposables.add(boxModel)
        boxVoxel = ModelInstance(boxModel)

        val playerModel = modelBuilder.createCapsule(0.5f, 2f, 16,
                Material(ColorAttribute.createDiffuse(1f, 1f, 1f, 1f),
                        ColorAttribute.createSpecular(1f, 1f, 1f, 1f),
                        FloatAttribute.createShininess(8f)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.Normal or VertexAttributes.Usage.TextureCoordinates).toLong())
        disposables.add(playerModel)

        player = Player(playerModel, 0f, 0f, 0f)
        playerMirror = Player(playerModel, 0f, 0f, 0f, true)
        loadLevel()
    }

    fun dispose() {
        disposables.forEach { it.dispose() }

        spriteBatch.dispose()
        modelBatch.dispose()
        font.dispose()
    }

    fun movePlayers(position: Vector3) {
        player.position.set(position)
        player.modelInstance.transform.setTranslation(position)
        playerMirror.position.set(position.x, position.y, -1f - position.z)
        playerMirror.modelInstance.transform.setTranslation(position.x, position.y, -1f - position.z)
    }

    fun moveObjective(position: Vector3) {
        if (voxelAt(position).type != VoxelType.Objective) {
            removeVoxel(position)
        }
        objective.setPosition(position)
        if (voxelAt(position.x, position.y, -1 - position.z).type != VoxelType.Objective) {
            removeVoxel(position.x, position.y, -1 - position.z)
        }
        objectiveMirror.setPosition(position.x, position.y, -1 - position.z)
    }

    fun loadLevel() {
        voxels.clear()

        val levelFile = Gdx.files.local("levels/${levels[currentLevel]}")
        val scanner = Scanner(levelFile.readString())
        var line = scanner.nextLine()
        while (scanner.hasNextLine()) {
            if (line.contains("player")) {
                line = scanner.nextLine()
                val firstSpaceSeparator = line.indexOf(" ")
                val secondSpaceSeparator = line.indexOf(" ", firstSpaceSeparator + 1)
                val x = line.substring(0, firstSpaceSeparator).toFloat()
                val y = line.substring(firstSpaceSeparator + 1, secondSpaceSeparator).toFloat()
                val z = line.substring(secondSpaceSeparator + 1).toFloat()
                playerOrigin.set(x, y, z)
                line = scanner.nextLine()
            }
            if (line.contains("objective")) {
                line = scanner.nextLine()
                val firstSpaceSeparator = line.indexOf(" ")
                val secondSpaceSeparator = line.indexOf(" ", firstSpaceSeparator + 1)
                val x = line.substring(0, firstSpaceSeparator).toFloat()
                val y = line.substring(firstSpaceSeparator + 1, secondSpaceSeparator).toFloat()
                val z = line.substring(secondSpaceSeparator + 1).toFloat()
                objectiveOrigin.set(x, y, z)
                objective = addVoxel(x, y, z, VoxelType.Objective)
                objectiveMirror = addVoxel(x, y, -1 - z, VoxelType.Objective)
                line = scanner.nextLine()
            }
            if (line.contains("box")) {
                while (scanner.hasNextLine()) {
                    line = scanner.nextLine()
                    if (!line[0].isDigit() && !line[1].isDigit()) {
                        break
                    }
                    val firstSpaceSeparator = line.indexOf(" ")
                    val secondSpaceSeparator = line.indexOf(" ", firstSpaceSeparator + 1)
                    val x = line.substring(0, firstSpaceSeparator).toInt()
                    val y = line.substring(firstSpaceSeparator + 1, secondSpaceSeparator).toInt()
                    val z = line.substring(secondSpaceSeparator + 1).toInt()
                    addVoxel(x, y, z, VoxelType.Box)
                }
            }
            if (line.contains("ground")) {
                while (scanner.hasNextLine()) {
                    line = scanner.nextLine()
                    if (!line[0].isDigit() && !line[1].isDigit()) {
                        break
                    }
                    val firstSpaceSeparator = line.indexOf(" ")
                    val secondSpaceSeparator = line.indexOf(" ", firstSpaceSeparator + 1)
                    val x = line.substring(0, firstSpaceSeparator).toInt()
                    val y = line.substring(firstSpaceSeparator + 1, secondSpaceSeparator).toInt()
                    val z = line.substring(secondSpaceSeparator + 1).toInt()
                    addVoxel(x, y, z)
                }
            }
        }
        scanner.close()

        player.history.clear()
        playerMirror.history.clear()

        movePlayers(playerOrigin)
    }

    fun saveLevel() {
        val levelFile = Gdx.files.local("levels/${levels[currentLevel]}")
        levelFile.writeString("", false)

        levelFile.writeString("player origin\n", true)
        levelFile.writeString("${playerOrigin.x.toInt()} ${playerOrigin.y.toInt()} ${playerOrigin.z.toInt()}\n", true)

        levelFile.writeString("objective origin\n", true)
        levelFile.writeString("${objectiveOrigin.x.toInt()} ${objectiveOrigin.y.toInt()} ${objectiveOrigin.z.toInt()}\n", true)

        levelFile.writeString("ground voxels\n", true)
        voxels.forEach {
            if (it.type == VoxelType.Ground) {
                levelFile.writeString("${it.x} ${it.y} ${it.z}\n", true)
            }
        }
        levelFile.writeString("box voxels\n", true)
        voxels.forEach {
            if (it.type == VoxelType.Box) {
                levelFile.writeString("${it.x} ${it.y} ${it.z}\n", true)
            }
        }
    }

    fun drawGame() {
        voxels.forEach {
            when (it.type) {
                VoxelType.Ground -> {
//                    if (!it.touched) {
                        groundVoxel.transform.setTranslation(it.x.toFloat(), it.y.toFloat(), it.z.toFloat())
                        modelBatch.render(groundVoxel, environment)
//                    } else {
//                        touchedGroundVoxel.transform.setTranslation(it.x.toFloat(), it.y.toFloat(), it.z.toFloat())
//                        modelBatch.render(touchedGroundVoxel, environment)
//                    }
                }
                VoxelType.Objective -> {
                    objectiveVoxel.transform.setTranslation(it.x.toFloat(), it.y.toFloat(), it.z.toFloat())
                    modelBatch.render(objectiveVoxel, environment)
                }
                VoxelType.Box -> {
                    boxVoxel.transform.setTranslation(it.x.toFloat(), it.y.toFloat(), it.z.toFloat())
                    modelBatch.render(boxVoxel, environment)
                }
                else -> {
                }
            }
        }
        modelBatch.render(player.modelInstance, environment)
        modelBatch.render(playerMirror.modelInstance, environment)
        modelBatch.render(mirror, environment)
    }

    val rewindDelay = 0.1f
    var rewindDelayTimer = 0f
    var winTimer = 0f
    fun updateNormal(deltaTime: Float) {
        var playersCanMove = true
        // WIN
        if (currentLevel == levels.lastIndex
                && objective.x == Math.round(player.position.x)
                && objective.y == Math.round(player.position.y) - 1
                && objective.z == Math.round(player.position.z)
                && objective.x == Math.round(playerMirror.position.x)
                && objective.y == Math.round(playerMirror.position.y) - 1
                && objective.z == Math.round(playerMirror.position.z)) {
            winTimer += deltaTime
            if (winTimer > 1f) {
                winTimer = 0f
                state = GameState.End
                playersCanMove = false
                currentLevel = 0
            }
        } else if (objective.x == Math.round(player.position.x)
                && objective.y == Math.round(player.position.y) - 1
                && objective.z == Math.round(player.position.z)
                && objectiveMirror.x == Math.round(playerMirror.position.x)
                && objectiveMirror.y == Math.round(playerMirror.position.y) - 1
                && objectiveMirror.z == Math.round(playerMirror.position.z)) {
            winTimer += deltaTime
            if (winTimer > 0.2f) {
                winTimer = 0f
                state = GameState.LevelTransition
                if (currentLevel < levels.lastIndex) {
                    currentLevel++
                }
                oldVoxels.addAll(voxels)
                oldVoxels.remove(objective)
                oldVoxels.remove(objectiveMirror)
                tmpVec3.set(player.position) // save player position
                loadLevel()
                tmpVec3.sub(player.position) // get difference
                tmpVec3.scl(-1f)
                camera3D.translate(tmpVec3)
                oldVoxels.forEach {
                    it.x += Math.round(tmpVec3.x)
                    it.y += Math.round(tmpVec3.y)
                    if (it.x == 0 && it.y == 0 && it.z == 0) {
                        it.z += 1
                    }
                }
                voxels.forEach {
                    if (it.x == 0 && it.y == 0 && it.z == 0) {
                        it.z += 1
                        zeroVoxel = it
                    }
                }
                playersCanMove = false
            }
        } else {
            winTimer = 0f
        }
//        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
//            state = GameState.Editor
//            Gdx.input.inputProcessor = camController
//        }

        // REWIND
        if (rewindDelayTimer > 0f) {
            rewindDelayTimer -= deltaTime
        } else if (Gdx.input.isKeyPressed(Input.Keys.Z) && !player.history.isEmpty()) {
            rewindDelayTimer = rewindDelay
            player.rewind()
            playerMirror.rewind()
            voxels.forEach {
                if (it.history != null) {
                    it.setPosition(it.history.last())
                    it.history.removeAt(it.history.lastIndex)
                }
            }
            playersCanMove = false
        }
        // RESET
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            movePlayers(playerOrigin)
            moveObjective(objectiveOrigin)
            player.history.clear()
            playerMirror.history.clear()
            voxels.forEach {
                if (it.history != null && !it.history.isEmpty()) {
                    it.setPosition(it.history.first())
                    it.history.clear()
                    it.history.add(Vector3(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()))
                }
//                it.touched = false
            }
            playersCanMove = false
        }

        if (playersCanMove) {
            anyPlayerFalling = false // reset before checking
            player.checkFalling()
            playerMirror.checkFalling()
            player.move(deltaTime)
            playerMirror.move(deltaTime)
        }

        voxels.forEach {
            if (it.type != VoxelType.Ground
                    && voxelAt(it.x, it.y - 1, it.z).type == VoxelType.Empty
                    && it.y > yMin) {
                it.y--
            }
        }

        if (updateVoxelHistory) {
            updateVoxelHistory = false
            voxels.forEach {
                if (!it.updated) {
                    it.history?.add(Vector3(it.x.toFloat(), it.y.toFloat(), it.z.toFloat()))
                }
                it.updated = false
            }
        }

        if (cameraFollowsPlayer) {
            tmpVec3.set(player.position)
            tmpVec3.add(cameraOffset)
            camera3D.position.lerp(tmpVec3, 0.05f)
            tmpVec3.set(camera3D.position)
            tmpVec3.sub(cameraOffset)
            camera3D.lookAt(tmpVec3)
        }

        camera3D.update()

        // RENDER
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glViewport(viewport.x, viewport.y, viewport.w, viewport.h)

        modelBatch.begin(camera3D)
        drawGame()
        modelBatch.end()

        if (currentLevel == 0) {
            spriteBatch.begin()
            font.draw(spriteBatch, "Arrow keys - move", 10f, viewport.h.toFloat() - 10f)
            font.draw(spriteBatch, "Z - rewind", 10f, viewport.h - 40f)
            font.draw(spriteBatch, "R - restart", 10f, viewport.h - 70f)
            spriteBatch.end()
        }
    }

    var transitionTimer = 0.1f
    var zeroVoxel: Voxel? = null
    fun updateLevelTransition(deltaTime: Float) {
        transitionTimer += deltaTime

        if (cameraFollowsPlayer) {
            tmpVec3.set(player.position)
            tmpVec3.add(cameraOffset)
            camera3D.position.lerp(tmpVec3, 0.05f)
            tmpVec3.set(camera3D.position)
            tmpVec3.sub(cameraOffset)
            camera3D.lookAt(tmpVec3)
        }

        // RENDER
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glViewport(viewport.x, viewport.y, viewport.w, viewport.h)

        modelBatch.begin(camera3D)
        if (transitionTimer < 2f) {
            val offset = MathUtils.lerp(1f, 30f, transitionTimer / 2f)
            oldVoxels.forEach {
                when (it.type) {
                    VoxelType.Ground -> {
                        groundVoxel.transform.setTranslation(it.x * offset, it.y * offset, it.z * offset)
                        modelBatch.render(groundVoxel, environment)
                    }
                    VoxelType.Objective -> {
                        objectiveVoxel.transform.setTranslation(it.x * offset, it.y * offset, it.z * offset)
                        modelBatch.render(objectiveVoxel, environment)
                    }
                    VoxelType.Box -> {
                        boxVoxel.transform.setTranslation(it.x * offset, it.y * offset, it.z * offset)
                        modelBatch.render(boxVoxel, environment)
                    }
                }
            }
        } else if (transitionTimer < 4f) {
            val offset = MathUtils.lerp(30f, 1f, (transitionTimer - 2f) / 2f)
            voxels.forEach {
                when (it.type) {
                    VoxelType.Ground -> {
                        groundVoxel.transform.setTranslation(it.x * offset, it.y * offset, it.z * offset)
                        modelBatch.render(groundVoxel, environment)
                    }
                    VoxelType.Objective -> {
                        objectiveVoxel.transform.setTranslation(it.x * offset, it.y * offset, it.z * offset)
                        modelBatch.render(objectiveVoxel, environment)
                    }
                    VoxelType.Box -> {
                        boxVoxel.transform.setTranslation(it.x * offset, it.y * offset, it.z * offset)
                        modelBatch.render(boxVoxel, environment)
                    }
                }
            }
        } else {
            state = GameState.Normal
            transitionTimer = 0.1f
            if (zeroVoxel != null) {
                zeroVoxel!!.z-- // restore zero voxel
                zeroVoxel = null
            }
        }
        // fake objectives
        objectiveVoxel.transform.setTranslation(player.position)
        objectiveVoxel.transform.translate(0f, -1f, 0f)
        modelBatch.render(objectiveVoxel, environment)
        objectiveVoxel.transform.setTranslation(playerMirror.position)
        objectiveVoxel.transform.translate(0f, -1f, 0f)
        modelBatch.render(objectiveVoxel, environment)
        modelBatch.render(player.modelInstance, environment)
        modelBatch.render(playerMirror.modelInstance, environment)
        modelBatch.render(mirror, environment)
        modelBatch.end()
    }

    fun updateEnd(deltaTime: Float) {
        // RENDER
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glViewport(viewport.x, viewport.y, viewport.w, viewport.h)

        spriteBatch.begin()
        spriteBatch.draw(end, 0f, 0f)
        spriteBatch.end()
    }

    var clickedVoxelFace = Vector3()
    var canClick = true
    fun updateEditor(deltaTime: Float) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            state = GameState.Normal
            Gdx.input.inputProcessor = null
        }

        // ADD / DESTROY VOXEL
        if (!Gdx.input.isTouched) {
            canClick = true
        }
        if ((canClick || !precision) && Gdx.input.isTouched) {
            canClick = false

            val ray = camera3D.getPickRay(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
            var clickedVoxel = nullVoxel
            var closestDistance = 100000000f
            val boundingBox = BoundingBox()

            voxels.forEach {
                if (it.type != VoxelType.Empty) {
                    tmpVec3.set(it.x.toFloat(), it.y.toFloat(), it.z.toFloat())
                    tmpVec3.sub(0.5f)
                    tmpVec3_2.set(tmpVec3)
                    tmpVec3_2.add(1f)
                    boundingBox.set(tmpVec3, tmpVec3_2)
                    if (Intersector.intersectRayBounds(ray, boundingBox, tmpVec3)) {
                        val distance = tmpVec3.dst2(ray.origin)
                        if (distance < closestDistance) {
                            clickedVoxel = it
                            closestDistance = distance
                            boundingBox.getCenter(tmpVec3_2)
                            tmpVec3.sub(tmpVec3_2).nor()
                            clickedVoxelFace.set(tmpVec3)
                        }
                    }
                }
            }

            if (setPlayerOrigin) {
                playerOrigin.set(clickedVoxel.x + Math.round(clickedVoxelFace.x).toFloat(),
                        clickedVoxel.y + Math.round(clickedVoxelFace.y).toFloat(),
                        clickedVoxel.z + Math.round(clickedVoxelFace.z).toFloat())
                movePlayers(playerOrigin)
                setPlayerOrigin = false
            } else if (setObjective) {
                objectiveOrigin.set(clickedVoxel.x.toFloat(), clickedVoxel.y.toFloat(), clickedVoxel.z.toFloat())
                moveObjective(objectiveOrigin)
                objective.history!!.clear()
                objectiveMirror.history!!.clear()
                objective.history!!.add(objectiveOrigin.cpy())
                objectiveMirror.history!!.add(Vector3(objectiveOrigin.x, objectiveOrigin.y, -1f - objectiveOrigin.z))
                setObjective = false
            } else if (addBox) {
                addVoxel(clickedVoxel.x + Math.round(clickedVoxelFace.x).toFloat(),
                        clickedVoxel.y + Math.round(clickedVoxelFace.y).toFloat(),
                        clickedVoxel.z + Math.round(clickedVoxelFace.z).toFloat(),
                        VoxelType.Box)
                addBox = false
            } else {
                if (clickedVoxel.type != VoxelType.Empty) {
                    if (Gdx.input.isButtonPressed(Input.Buttons.RIGHT)) {
                        voxels.remove(clickedVoxel)
                    } else if (Gdx.input.isButtonPressed(Input.Buttons.LEFT)) {
                        addVoxel(clickedVoxel.x + Math.round(clickedVoxelFace.x), clickedVoxel.y + Math.round(clickedVoxelFace.y), clickedVoxel.z + Math.round(clickedVoxelFace.z))
                    }
                }
            }

            saveLevel()
        }

        editorButtons.forEach { it.update() }

        camController.update()
        camera3D.update()

        // RENDER
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT or GL20.GL_DEPTH_BUFFER_BIT)
        Gdx.gl.glViewport(viewport.x, viewport.y, viewport.w, viewport.h)

        modelBatch.begin(camera3D)
        drawGame()
        modelBatch.end()

        spriteBatch.begin()
//        editorButtons.forEach { it.draw(spriteBatch, font) }
        spriteBatch.end()
    }

    fun update(deltaTime: Float) {
        when (state) {
            GameState.Normal -> updateNormal(deltaTime)
            GameState.LevelTransition -> updateLevelTransition(deltaTime)
            GameState.End -> updateEnd(deltaTime)
            GameState.Editor -> updateEditor(deltaTime)
        }

        if (Gdx.input.isKeyPressed(Input.Keys.ESCAPE)) {
            preferences.putInteger("currentLevel", currentLevel)
            preferences.flush()
            Gdx.app.exit()
        }
    }
}