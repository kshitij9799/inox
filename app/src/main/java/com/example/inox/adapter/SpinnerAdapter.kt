package com.example.inox.adapter

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class SpinnerAdapter(context: Context, items: List<String>, private val disabledPosition: Int) :
    ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, items) {

    override fun isEnabled(position: Int): Boolean {
        return position != disabledPosition
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        val textView = view.findViewById<TextView>(android.R.id.text1)
        if (position == disabledPosition) {
            textView.visibility = View.GONE
        }
        return view
    }
}