package in.androidapps.parv.digitalleash_parent;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class DangerActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_danger);
    }
    public void backFar(View view){
/*

        Intent intent= new Intent(this,MainActivity.class);
        startActivity(intent);

        when you define intent in safe/dangerAcitivity then it
          ends withoverlapping like layer over layer as click back button on the phone
*/

        finish();


    }
}
