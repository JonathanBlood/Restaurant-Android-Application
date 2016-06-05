package com.jonathanblood.restaurant;

import android.app.ProgressDialog;
import android.support.v7.app.AppCompatActivity;

public class BaseActivity extends AppCompatActivity
{

    private ProgressDialog mProgressDialog;

    public void showProgressDialog()
    {
        if (mProgressDialog == null)
        {
            mProgressDialog = new ProgressDialog(this);
            mProgressDialog.setMessage(getString(R.string.loading));
            mProgressDialog.setIndeterminate(true);
        }

        mProgressDialog.show();
    }

    public void hideProgressDialog()
    {
        if (mProgressDialog != null && mProgressDialog.isShowing())
        {
            mProgressDialog.hide();
        }
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        hideProgressDialog();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        hideProgressDialog();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        hideProgressDialog();
    }


}