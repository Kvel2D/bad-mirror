package com.mygdx.core

import com.badlogic.gdx.math.Vector3

enum class VoxelType { Ground, Empty, Objective, Box }
class Voxel {
    var x: Int
    var y: Int
    var z: Int
//    var touched = false
    var updated = false
    var type: VoxelType
    val history: MutableList<Vector3>?

    constructor(x: Int, y: Int, z: Int, type: VoxelType = VoxelType.Empty) {
        this.x = x
        this.y = y
        this.z = z
        this.type = type
        if (type == VoxelType.Objective || type == VoxelType.Box) {
            history = mutableListOf(Vector3(x.toFloat(), y.toFloat(), z.toFloat()))
        } else {
            history = null
        }
    }

    fun move(distance: Vector3) {
        x += Math.round(distance.x)
        y += Math.round(distance.y)
        z += Math.round(distance.z)
    }

    fun setPosition(position: Vector3) {
        x = Math.round(position.x)
        y = Math.round(position.y)
        z = Math.round(position.z)
    }

    fun setPosition(x: Float, y: Float, z: Float) {
        this.x = Math.round(x)
        this.y = Math.round(y)
        this.z = Math.round(z)
    }
}


val voxels = mutableListOf<Voxel>()
val oldVoxels = mutableListOf<Voxel>()
val nullVoxel = Voxel(0, 0, 0)
var updateVoxelHistory = false

fun voxelAt(position: Vector3): Voxel {
    return voxelAt(Math.round(position.x), Math.round(position.y), Math.round(position.z))
}

fun voxelAt(x: Float, y: Float, z: Float): Voxel {
    return voxelAt(Math.round(x), Math.round(y), Math.round(z))
}

fun voxelAt(x: Int, y: Int, z: Int): Voxel {
    voxels.forEach {
        if (it.x == x && it.y == y && it.z == z) {
            return it
        }
    }
    return nullVoxel
}

fun removeVoxel(position: Vector3) {
    removeVoxel(position.x.toInt(), position.y.toInt(), position.z.toInt())
}

fun removeVoxel(x: Float, y: Float, z: Float) {
    voxels.remove(voxelAt(x.toInt(), y.toInt(), z.toInt()))
}

fun removeVoxel(x: Int, y: Int, z: Int) {
    voxels.remove(voxelAt(x, y, z))
}

fun addVoxel(position: Vector3, type: VoxelType = VoxelType.Ground): Voxel {
    return addVoxel(position.x.toInt(), position.y.toInt(), position.z.toInt(), type)
}

fun addVoxel(x: Float, y: Float, z: Float, type: VoxelType = VoxelType.Ground): Voxel {
    return addVoxel(x.toInt(), y.toInt(), z.toInt(), type)
}

fun addVoxel(x: Int, y: Int, z: Int, type: VoxelType = VoxelType.Ground): Voxel {
    val currentVoxel = voxelAt(x, y, z)

    if (currentVoxel.type == VoxelType.Empty) {
        val newVoxel = Voxel(x, y, z, type)
        voxels.add(newVoxel)
        return newVoxel
    } else {
        println("There's already ${currentVoxel.type} voxel at $x, $y, $z")
        return currentVoxel
    }
}