package com.aish.arseup

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.collision.Box
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ViewRenderable
import com.google.ar.sceneform.ux.ArFragment
import com.google.ar.sceneform.ux.TransformableNode
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.CompletableFuture

private const val BOTTOMSHEET_PEEKHEIGHT = 50f

class MainActivity : AppCompatActivity() {

    private val models = mutableListOf(
        Model(R.drawable.chair, "Chair", R.raw.chair),
        Model(R.drawable.couch, "Couch", R.raw.couch),
        Model(R.drawable.table, "Table", R.raw.table),
        Model(R.drawable.oven, "Oven", R.raw.oven),
        Model(R.drawable.piano, "Piano", R.raw.piano)
    )
    private lateinit var selectedFurniture: Model
    private lateinit var arFragment: ArFragment

    val viewNodes = mutableListOf<Node>()
    var bottomSheetBehavior: BottomSheetBehavior<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        arFragment = fragment as ArFragment
        setupBottomSheet()

        setupRecyclerView()
        setupDopubleTapARPlaneListener()

        getCurrentScene().addOnUpdateListener {
            rotateViewNodesTowardsUser()
        }
    }

    private fun setupDopubleTapARPlaneListener() {
        arFragment.setOnTapArPlaneListener { hitResult, plane, motionEvent ->
            loadModel { modelRenderable, viewRenderable ->
                addNodeToScene(hitResult.createAnchor(), modelRenderable, viewRenderable)
            }
        }
    }


    private fun setupBottomSheet() {
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior?.peekHeight = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, BOTTOMSHEET_PEEKHEIGHT, resources.displayMetrics
        ).toInt()

        bottomSheetBehavior?.addBottomSheetCallback(object :
            BottomSheetBehavior.BottomSheetCallback() {
            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                bottomSheet.bringToFront()
            }

            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }
        })
    }

    private fun setupRecyclerView() {
        rvModels.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvModels.adapter = models?.let {
            ModelAdapter(it).apply {
                selectedFurniture.observe(this@MainActivity, Observer { model ->
                    this@MainActivity.selectedFurniture = model
                    val newTitle = "You are looking at ${model.title}"
                    tvModel.text = newTitle
                    bottomSheetBehavior?.state = BottomSheetBehavior.STATE_COLLAPSED
                })
            }
        }
    }

    private fun createDeleteButton(): Button {
        return Button(this).apply {
            text = context.getString(R.string.delete)
            setBackgroundColor(Color.RED)
            setTextColor(Color.WHITE)
        }
    }

    private fun getCurrentScene(): Scene = arFragment.arSceneView.scene

    private fun rotateViewNodesTowardsUser() {
        for (node in viewNodes) {
            node.renderable?.let {
                val cameraPosition = getCurrentScene().camera.worldPosition
                val viewNodePosition = node.worldPosition
                val dir = Vector3.subtract(cameraPosition, viewNodePosition)
                node.worldRotation = Quaternion.lookRotation(dir, Vector3.up())
            }
        }
    }

    private fun addNodeToScene(
        anchor: Anchor,
        modelRenderable: ModelRenderable,
        viewRenderable: ViewRenderable
    ) {
        val anchorNode = AnchorNode(anchor)

        val modelNode = TransformableNode(arFragment.transformationSystem).apply {
            renderable = modelRenderable
            setParent(anchorNode)
            getCurrentScene().addChild(anchorNode)
            select()
        }

        val viewNode = Node().apply {
            renderable = null
            setParent(modelNode)
            val box = modelNode.renderable?.collisionShape as Box
            localPosition = Vector3(0f, box.size.y, 0f)
            (viewRenderable.view as Button).setOnClickListener {
                getCurrentScene().removeChild(anchorNode)
                viewNodes.remove(this)
            }
        }

        viewNodes.add(viewNode)
        modelNode.setOnTapListener { _, _ ->
            if(!modelNode.isTransforming) {
                if(viewNode.renderable == null) {
                    viewNode.renderable = viewRenderable
                } else {
                    viewNode.renderable = null
                }
            }
        }
    }

    private fun loadModel(callback: (ModelRenderable, ViewRenderable) -> Unit) {
        val modelRenderable = ModelRenderable.builder()
            .setSource(this, selectedFurniture.modelId)
            .build()

        val viewRenderable = ViewRenderable.builder()
            .setView(this, createDeleteButton())
            .build()

        CompletableFuture.allOf(modelRenderable, viewRenderable)
            .thenAccept {
                callback(modelRenderable.get(), viewRenderable.get())
            }.exceptionally {
                Toast.makeText(this, "Error loading furniture $it", Toast.LENGTH_SHORT).show()
                null
            }
    }

}