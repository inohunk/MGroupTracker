package ru.hunkel.mgrouptracker.fragments


import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.fragment_error.view.*
import ru.hunkel.mgrouptracker.R
import ru.hunkel.mgrouptracker.utils.ErrorCodes

class ErrorFragment(private val errorCode: Int) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(activity!!)
        val errorText = ErrorCodes().getErrorByCode(errorCode)

        val view = LayoutInflater.from(context).inflate(R.layout.fragment_error, null)
        view.message_type_text_view.text = errorText!!.name
        view.message_text_text_view.text = errorText!!.description
        builder.setView(view)
            .setPositiveButton(
                "Выйти"
            ) { _, _ ->
                dismiss()
                activity?.finish()
            }
        val dialog = builder.create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(resources.getColor(R.color.colorError))
        }
        return dialog
    }

    override fun onCancel(dialog: DialogInterface) {
        activity!!.finish()
    }
}
