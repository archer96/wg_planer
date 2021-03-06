package de.ameyering.wgplaner.wgplaner.section.settings;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.Random;
import java.util.regex.Pattern;

import de.ameyering.wgplaner.wgplaner.R;
import de.ameyering.wgplaner.wgplaner.WGPlanerApplication;
import de.ameyering.wgplaner.wgplaner.customview.CircularImageView;
import de.ameyering.wgplaner.wgplaner.utils.DataProvider;
import de.ameyering.wgplaner.wgplaner.utils.DataProviderInterface;
import de.ameyering.wgplaner.wgplaner.utils.OnAsyncCallListener;
import de.ameyering.wgplaner.wgplaner.utils.ServerCallsInterface;
import io.swagger.client.ApiException;
import io.swagger.client.model.SuccessResponse;

public class ProfileSettingsActivity extends AppCompatActivity {
    public static final int REQ_CODE_PICK_IMAGE = 0;
    private static int standard_width = 512;
    private static int standard_text_size = 300;

    private DataProviderInterface dataProvider;

    private Button btLeaveGroup;
    private EditText inputName;
    private EditText inputEmail;
    private CircularImageView image;
    private Menu menu;

    private Bitmap bitmap;
    private Uri selectedImage;

    private boolean isInEditMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_settings);

        WGPlanerApplication application = (WGPlanerApplication) getApplication();
        dataProvider = application.getDataProviderInterface();

        Toolbar toolbar = findViewById(R.id.profile_settings_toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_black);
        toolbar.setNavigationOnClickListener(view -> {
            if (isInEditMode) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ProfileSettingsActivity.this);
                builder.setMessage(getString(R.string.dialog_discard_message));
                builder.setPositiveButton(R.string.dialog_discard_positive, (dialogInterface, i) -> {
                    dialogInterface.cancel();
                    setResult(RESULT_CANCELED);
                    finish();
                });
                builder.setNegativeButton(R.string.dialog_discard_negative, (dialogInterface,
                        i) -> dialogInterface.cancel());
                AlertDialog dialog = builder.create();
                dialog.show();

            } else {
                setResult(RESULT_CANCELED);
                finish();
            }
        });

        //Choose image
        image = findViewById(R.id.profile_settings_profile_picture);
        image.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            CharSequence[] options = new CharSequence[2];
            options[0] = getString(R.string.pick_image);
            options[1] = getString(R.string.generate_image);
            builder.setItems(options, (dialogInterface, i) -> {
                if (i == 0) {
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQ_CODE_PICK_IMAGE);

                } else if (i == 1) {
                    bitmap = createStandardBitmap(dataProvider.getCurrentUserDisplayName());

                    runOnUiThread(() -> {
                        image.setImageBitmap(bitmap);
                        image.startAnimation(AnimationUtils.loadAnimation(this,
                                R.anim.anim_load_new_profile_picture));
                    });
                }
            });

            builder.show();
        });
        image.setEnabled(false);

        image.setImageBitmap(dataProvider.getCurrentUserImage());

        btLeaveGroup = findViewById(R.id.bt_delete_group_profile_settings);

        inputName = findViewById(R.id.tfName_profile_settings);
        String displayName = dataProvider.getCurrentUserDisplayName();

        if (displayName != null) {
            inputName.setText(displayName);
        }

        inputEmail = findViewById(R.id.tfEmail_profile_settings);
        String email = dataProvider.getCurrentUserEmail();

        if (email != null) {
            inputEmail.setText(email);
        }

        btLeaveGroup.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(ProfileSettingsActivity.this);
            builder.setTitle(getString(R.string.dialog_leave_group_title));
            builder.setMessage(getString(R.string.dialog_leave_group_message));

            builder.setPositiveButton(R.string.dialog_leave_group_positive,
            (dialogInterface, i) -> {
                dataProvider.leaveCurrentGroup(new OnAsyncCallListener<SuccessResponse>() {
                    @Override
                    public void onFailure(ApiException e) {
                        runOnUiThread(() -> {
                            Toast.makeText(ProfileSettingsActivity.this, getString(R.string.server_connection_failed),
                                Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onSuccess(SuccessResponse result) {
                        runOnUiThread(() -> {
                            setResult(RESULT_OK);
                            finish();
                        });
                    }
                });
            });

            builder.setNegativeButton(R.string.dialog_discard_negative, (dialogInterface1, i1) -> dialogInterface1.cancel());

            builder.create().show();
        });
    }

    private Bitmap createStandardBitmap(String displayName) {
        Bitmap standard = Bitmap.createBitmap(standard_width, standard_width, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(standard);
        Paint paint = new Paint();

        Random random = new Random();
        int randomRed = random.nextInt(230);
        int randomGreen = random.nextInt(230);
        int randomBlue = random.nextInt(230);

        int color = Color.argb(255, randomRed, randomGreen, randomBlue);

        paint.setColor(color);

        canvas.drawRect(0, 0, standard_width, standard_width, paint);


        Paint textPaint = new Paint();
        textPaint.setARGB(255, 255, 255, 255);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(standard_text_size);

        int xPos = (canvas.getWidth() / 2);
        int yPos = (int)((canvas.getHeight() / 2) - ((textPaint.descent() + textPaint.ascent()) / 2));

        canvas.drawText(displayName.substring(0, 1), xPos, yPos, textPaint);

        return standard;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    selectedImage = data.getData();

                    try {
                        bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImage);
                        bitmap = scaleBitmap(bitmap);

                        image.setImageBitmap(bitmap);
                        image.startAnimation(AnimationUtils.loadAnimation(this,
                                R.anim.anim_load_new_profile_picture));

                    } catch (IOException e) {
                        Toast.makeText(this, "Failed to load Picture", Toast.LENGTH_LONG).show();
                    }
                }

                break;
        }
    }

    private Bitmap scaleBitmap(Bitmap bitmap) {
        int maxLength = Math.max(bitmap.getHeight(), bitmap.getWidth());
        float scale = (float) 800 / (float) maxLength;

        int newWidth = Math.round(bitmap.getWidth() * scale);
        int newHeight = Math.round(bitmap.getHeight() * scale);

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.menu_edit_full_screen_actvity, menu);
        MenuItem save = menu.findItem(R.id.edit_fullscreen_save);
        MenuItem edit = menu.findItem(R.id.edit_fullscreen_edit);

        if (isInEditMode) {
            save.setVisible(true);
            edit.setVisible(false);

        } else {
            edit.setVisible(true);
            save.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_fullscreen_save: {
                if (checkInputAndReturn()) {
                    dataProvider.setCurrentUserDisplayName(inputName.getText().toString(), null);
                    dataProvider.setCurrentUserEmail(inputEmail.getText().toString(), null);
                    dataProvider.setCurrentUserImage(bitmap, null);
                    Intent data = new Intent();
                    setResult(RESULT_OK, data);
                    finish();
                    return true;
                }
            }

            case R.id.edit_fullscreen_edit: {
                isInEditMode = true;
                item.setVisible(false);
                MenuItem save = menu.findItem(R.id.edit_fullscreen_save);
                save.setVisible(true);
                this.inputName.setEnabled(true);
                this.image.setEnabled(true);
                Resources r = getResources();
                float elevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 5f, r.getDisplayMetrics());
                ObjectAnimator.ofFloat(image, "elevation", elevation).setDuration(200).start();
                this.inputEmail.setEnabled(true);
                return true;
            }
        }

        return false;
    }

    private boolean checkInputAndReturn() {
        String displayName = inputName.getText().toString();

        if (displayName.isEmpty()) {
            Toast.makeText(this, getString(R.string.delete_display_name_error), Toast.LENGTH_LONG).show();
            return false;
        }

        String email = inputEmail.getText().toString();

        if (!email.isEmpty()) {
            if (!isValidEmail(email)) {
                Toast.makeText(this, getString(R.string.invalid_email), Toast.LENGTH_LONG).show();
                return false;
            }
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        String regex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
        Pattern pattern = Pattern.compile(regex);

        return pattern.matcher(email).matches();
    }
}
