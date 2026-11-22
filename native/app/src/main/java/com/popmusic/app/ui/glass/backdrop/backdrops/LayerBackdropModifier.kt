/*
 * Vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 *
 * Vendored so the library ships as source with this app (binary AARs compiled
 * against older Compose broke at runtime) and to add a backdrop resolution
 * scale for cheaper effect rendering. KMP expect/actual declarations were
 * merged into this single Android source set. Package renamed accordingly.
 */
package com.popmusic.app.ui.glass.backdrop.backdrops

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toIntSize
import com.popmusic.app.ui.glass.backdrop.internal.recordLayer

fun Modifier.layerBackdrop(backdrop: LayerBackdrop): Modifier =
  this then LayerBackdropElement(backdrop)

private class LayerBackdropElement(val backdrop: LayerBackdrop) :
  ModifierNodeElement<LayerBackdropNode>() {

  override fun create(): LayerBackdropNode {
    return LayerBackdropNode(backdrop)
  }

  override fun update(node: LayerBackdropNode) {
    if (node.backdrop != backdrop) {
      node.backdrop.layerCoordinates = null
      node.backdrop = backdrop
      node.invalidateRecord()
    }
    node.invalidateDraw()
  }

  override fun InspectorInfo.inspectableProperties() {
    name = "layerBackdrop"
    properties["backdrop"] = backdrop
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is LayerBackdropElement) return false

    if (backdrop != other.backdrop) return false

    return true
  }

  override fun hashCode(): Int {
    return backdrop.hashCode()
  }
}

private class LayerBackdropNode(var backdrop: LayerBackdrop) :
  DrawModifierNode, GlobalPositionAwareModifierNode, Modifier.Node() {

  private var recordedSize: IntSize? = null
  private var recordedVersion: Int = -1

  override fun ContentDrawScope.draw() {
    val size = this.size.toIntSize()
    val version = backdrop.contentVersion
    if (backdrop.cacheContent && recordedSize == size && recordedVersion == version) {
      drawLayer(backdrop.graphicsLayer)
      return
    }
    drawContent()
    if (!backdrop.cacheContent || recordedSize != size || recordedVersion != version) {
      recordLayer(this@LayerBackdropNode, backdrop.graphicsLayer, size) {
        backdrop.onDraw(this@draw)
      }
      recordedSize = size
      recordedVersion = version
    }
  }

  fun invalidateRecord() {
    recordedSize = null
  }

  override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
    if (coordinates.isAttached) {
      backdrop.layerCoordinates = coordinates
    }
  }

  override fun onDetach() {
    backdrop.layerCoordinates = null
    recordedSize = null
  }
}
