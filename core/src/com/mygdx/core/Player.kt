package com.mygdx.core

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelInstance
import com.badlogic.gdx.math.Vector3
import com.mygdx.core.*

const val fallSpeed = 0.1f
const val bobMax = 0.2f
const val bobDecreaseSpeed = 0.03f
const val moveDelay = 0.1f
var anyPlayerFalling = false

class Player {
    val modelInstance: ModelInstance
    val position: Vector3
    val reversed: Boolean
    val history = mutableListOf<Vector3>()
    var currentBob = 0f
    var falling = false

    constructor(model: Model, x: Float, y: Float, z: Float, reversed: Boolean = false) {
        position = Vector3(x, y, z)
        modelInstance = ModelInstance(model, position)
        this.reversed = reversed
    }

    fun checkFalling() {
        val voxelBelow = voxelAt(position.x, Math.ceil(position.y.toDouble()).toFloat() - 1f, position.z)

        if (voxelBelow.type == VoxelType.Empty) {
            falling = true
            anyPlayerFalling = true
        } else {
//            voxelBelow.touched = true
            falling = false
        }
    }

    var moveDelayTimer = 0f
    fun move(deltaTime: Float) {
        if (falling && position.y > yMin) {
            position.y -= fallSpeed
            modelInstance.transform.translate(0f, -fallSpeed, 0f)
        } else if (!anyPlayerFalling) {
            if (moveDelayTimer > 0f) {
                moveDelayTimer -= deltaTime
                if (!Gdx.input.isKeyPressed(Input.Keys.UP)
                        && !Gdx.input.isKeyPressed(Input.Keys.DOWN)
                        && !Gdx.input.isKeyPressed(Input.Keys.LEFT)
                        && !Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                    moveDelayTimer = 0f
                }
            } else {
                tmpVec3.setZero()
                if (Gdx.input.isKeyPressed(Input.Keys.UP)) {
                    tmpVec3.add(-1f, 0f, 0f)
                }
                if (Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
                    tmpVec3.add(1f, 0f, 0f)
                }
                if (reversed) {
                    if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                        tmpVec3.add(0f, 0f, 1f)
                    }
                    if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                        tmpVec3.add(0f, 0f, -1f)
                    }
                } else {
                    if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                        tmpVec3.add(0f, 0f, 1f)
                    }
                    if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                        tmpVec3.add(0f, 0f, -1f)
                    }
                }
                if (!tmpVec3.isZero) {
                    moveDelayTimer = moveDelay
                    currentBob = bobMax
                    updateVoxelHistory = true
                    history.add(position.cpy())
                }
                position.add(tmpVec3)

                val destinationVoxel = voxelAt(position)
                val destinationVoxelUpper = voxelAt(position.x, position.y + 1, position.z)
                if (destinationVoxel.type == VoxelType.Box || destinationVoxel.type == VoxelType.Objective) {
                    position.add(tmpVec3)
                    if (voxelAt(position).type != VoxelType.Empty
                            || ((position.x < 30f) && ((!reversed && position.z < 0f) || (reversed && position.z >= 0f)))) {
                        // no movement
                        position.sub(tmpVec3)
                        position.sub(tmpVec3)
                    } else {
                        // move box
                        position.sub(tmpVec3)
                        destinationVoxel.history!!.add(Vector3(destinationVoxel.x.toFloat(), destinationVoxel.y.toFloat(), destinationVoxel.z.toFloat()))
                        destinationVoxel.updated = true
                        destinationVoxel.move(tmpVec3)
                    }
                } else if (destinationVoxel.type != VoxelType.Empty || destinationVoxelUpper.type != VoxelType.Empty) {
//                    destinationVoxel.touched = true
//                    destinationVoxelUpper.touched = true
                    position.sub(tmpVec3)
                } else if ((position.x < 30f) && ((!reversed && position.z < 0f) || (reversed && position.z >= 0f))) {
                    position.sub(tmpVec3)
                }
            }
        }

        // make the player bob after moving
        if (currentBob > 0f) {
            currentBob -= bobDecreaseSpeed
        } else {
            currentBob = 0f
        }
        modelInstance.transform.setTranslation(position)
        modelInstance.transform.translate(0f, currentBob, 0f)
    }

    fun rewind() {
        position.set(history.last())
        modelInstance.transform.setTranslation(position)
        history.removeAt(history.lastIndex)
    }
}

