package com.tumuyan.ncnn.realsr;

import static com.tumuyan.ncnn.realsr.UriUntils.getFileName;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.icu.text.SimpleDateFormat;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int SELECT_IMAGE = 1, SELECT_MULTI_IMAGE = 2;
    private static final int MY_PERMISSIONS_REQUEST = 100;

    private static String CMD_RESET_CACHE = "cp /system/vendor/lib64/libOpenCL.so ./;rm *.cache;rm */*.cache;chmod 777 *; echo Cache has been reset.;ls";
    private int selectCommand = 0;
    private String threadCount = "";
    private SubsamplingScaleImageView imageView;
    private TextView logTextView;
    private boolean initProcess;
    private final String galleryPath =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                    + File.separator + "RealSR";
    private File outputFile, outputGif, inputFile, titleFile;
    private String dir, cache_dir;
    private String modelName = "SR";
    private SearchView searchView;
    private MenuItem menuProgress;
    private Spinner spinner;
    private Process process;
    private boolean newTask;
    private int format, name, name2, notify;
    private String BUSY, ERR, DONE;
    private String outputSavePath = "";
    private String inputFileName = "";

    // --- Auto Queue feature ---
    private final List<Uri> autoQueue = new ArrayList<>();
    private int autoQueueIndex = 0;
    private boolean queueRunning = false;

    // --- Preview toggle ---
    private boolean showPreview = true;
    private File currentPreviewFile = null;

    // UI elements for queue
    private TextView queueStatusText;
    private Button btnStartQueue, btnStopQueue, btnClearQueue, btnTogglePreview;

    private String[] formats;

    private String[] command = null;
    private String log = "";

    private final String[] bench_mark_commands = new String[]{
            "./realsr-ncnn -c 46 -i img/PM5544.jpeg -o input.png  -m models-Real-ESRGAN",
            "./realsr-ncnn -c 46 -i input.png -o output.png  -m models-Real-ESRGANv3-anime -s 4"
    };

    private final String[] command_0 = new String[]{
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGAN-anime",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGAN",
            "./realsr-ncnn -i input.png -o output.png  -m models-RealeSR-general-v3 -s 4",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGANv3-anime -s 2",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGANv3-anime -s 3",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGANv3-anime -s 4",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGANv2-anime -s 2",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGANv2-anime -s 4",
            "./mnnsr-ncnn -i input.png -o output.png  -m models-MNN/ESRGAN-MoeSR-jp_Illustration-x4.mnn -s 4",
            "./mnnsr-ncnn -i input.png -o output.png  -m models-MNN/ESRGAN-MoeSR-jp_Illustration-x4.mnn -d 0 -s 4",
            "./realsr-ncnn -i input.png -o output.png  -m models-ESRGAN-Nomos8kSC -s 4",
            "./mnnsr-ncnn -i input.png -o output.png  -m models-MNN/ESRGAN-Nomos8kSC-x4.mnn -s 4",
            "./realsr-ncnn -i input.png -o output.png  -m models-Real-ESRGAN-SourceBook -s 2",
            "./realcugan-ncnn -i input.png -o output.png  -m models-nose -s 2  -n 0",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 2  -n -1",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 2  -n 0",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 2  -n 1",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 2  -n 2",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 2  -n 3",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 4  -n -1",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 4  -n 0",
            "./realcugan-ncnn -i input.png -o output.png  -m models-se -s 4  -n 3",
            "./realcugan-ncnn -i input.png -o output.png  -m models-pro -s 2  -n -1",
            "./realcugan-ncnn -i input.png -o output.png  -m models-pro -s 2  -n 0",
            "./realcugan-ncnn -i input.png -o output.png  -m models-pro -s 2  -n 3",
            "./realcugan-ncnn -i input.png -o output.png  -m models-pro -s 3  -n -1",
            "./realcugan-ncnn -i input.png -o output.png  -m models-pro -s 3  -n 0",
            "./realcugan-ncnn -i input.png -o output.png  -m models-pro -s 3  -n 3",
            "./Anime4k -i input.png -o output.png -z 2 -A",
            "./Anime4k -i input.png -o output.png -z 2 -A -a -e 48",
            "./Anime4k -i input.png -o output.png -z 2 -A -b -r 48",
            "./Anime4k -i input.png -o output.png -z 2 -A -w",
            "./Anime4k -i input.png -o output.png -z 2 -A -w -H",
            "./Anime4k -i input.png -o output.png -z 4 -A ",
            "./Anime4k -i input.png -o output.png -z 4 -A -a -e 40",
            "./Anime4k -i input.png -o output.png -z 4 -A -b -r 40",
            "./Anime4k -i input.png -o output.png -z 4 -A -w",
            "./Anime4k -i input.png -o output.png -z 4 -A -w -H",
    };
    private int tileSize;
    private boolean useCPU;
    private boolean keepScreen;
    private boolean useMultFiles;
    private boolean prePng;
    private boolean preFrame;
    private boolean autoSave;
    private boolean showSearchView;
    private String savePath = galleryPath;
    private static final int NOTIFY_ID = 1;
    private static final String CHANNEL_NAME_RESULT = "channel_result";
    private static final String CHANNEL_NAME_PROGRESS = "channel_progress";

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menuProgress = menu.findItem(R.id.progress);
        if (initProcess) {
            initProcess = false;
            menuProgress.setTitle("");
            Log.i("onCreateOptionsMenu", "onCreate() done");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        final String q;
        String imageName = "/output.png";
        boolean bench_mark_mode = false;
        int v = item.getItemId();
        if (v == R.id.progress) {
            stopCommand();
            return false;
        } else if (v == R.id.menu_share) {
            if (inputIsGifAnimation)
                shareImage("output.gif");
            else
                shareImage("output.png");
            return false;
        } else if (v == R.id.menu_avir2) {
            q = "./resize-ncnn -i input.png -o output.png  -m avir -s 0.5";
        } else if (v == R.id.menu_nearest4) {
            q = "./resize-ncnn -i input.png -o output.png  -m nearest -s 4";
        } else if (v == R.id.menu_de_nearest) {
            q = "./resize-ncnn -i input.png -o output.png  -m de-nearest";
        } else if (v == R.id.menu_de_nearest2) {
            q = "./resize-ncnn -i input.png -o output.png  -m de-nearest2";
        } else if (v == R.id.menu_magick2) {
            q = "./magick input.png -resize 50% output.png";
        } else if (v == R.id.menu_magick3) {
            q = "./magick input.png -resize 33.33% output.png";
        } else if (v == R.id.menu_magick4) {
            q = "./magick input.png -resize 25% output.png";
        } else if (v == R.id.menu_out2in) {
            if (inputIsGifAnimation) {
                Toast.makeText(this, R.string.not_support_animation, Toast.LENGTH_SHORT);
                return false;
            } else {
                q = "cp output.png input.png";
                imageName = "/input.png";
            }
        } else if (v == R.id.menu_in) {
            q = "in";
        } else if (v == R.id.menu_out) {
            q = "out";
        } else if (v == R.id.menu_help) {
            q = "help";
        } else if (v == R.id.menu_reset_cache) {
            q = CMD_RESET_CACHE;
            imageName = "";
        } else if (v == R.id.menu_bench_mark) {
            String append_param = "";
            if (tileSize > 0)
                append_param = " -t " + tileSize;
            if (useCPU)
                append_param += (" -g -1");

            append_param += ";";
            q = "rm -rf *.png; ls *.png; " + bench_mark_commands[0] + append_param + bench_mark_commands[1] + append_param;

            imageName = "/img/realsr.png";
            bench_mark_mode = true;
            imageView.setVisibility(View.GONE);
            if (keepScreen) {
                logTextView.setKeepScreenOn(true);
            }
        } else
            q = "";

        if (!run_fake_command(q)) {
            stopCommand();
            String finalImageName = imageName;
            boolean final_bench_mark_mode = bench_mark_mode;
            new Thread(() -> {
                if (q == CMD_RESET_CACHE) {
                    AssetsCopyer.releaseAssets(this, "realsr", cache_dir, false);
                }

                run20(q, final_bench_mark_mode, false);
                final File finalfile = new File(dir + finalImageName);
                if (finalfile.exists() && (!finalfile.isDirectory())) {
                    runOnUiThread(() -> {
                        if (showPreview) {
                            imageView.setVisibility(View.VISIBLE);
                            imageView.setImage(ImageSource.uri(finalfile.getAbsolutePath()));
                        }
                        logTextView.setKeepScreenOn(false);
                    });
                } else {
                    runOnUiThread(() -> setPreviewVisibility(false));
                }
            }).start();
        }

        return super.onOptionsItemSelected(item);
    }

    // 删除文件或者目录
    public static void deleteFile(File f) {
        if (f.isDirectory()) {
            File[] files = f.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFile(file);
                } else {
                    file.delete();
                }
            }
        }
        f.delete();
    }

    public void shareImage(String path) {
        Intent share_intent = new Intent();

        Uri contentUri = null;
        File file = null;
        if (!outputSavePath.isEmpty()) {
            file = new File(outputSavePath);
            if (file.exists()) {
                contentUri = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        file);
            }
        }

        if (contentUri == null) {
            file = new File(dir, path);
            if (file.exists()) {
                contentUri = FileProvider.getUriForFile(this,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        file);
            }
        }

        if (contentUri != null) {
            String suffix = file.getName().replaceFirst(".+\\.([^.]+)$", "$1").toLowerCase(Locale.ROOT);
            switch (suffix) {
                case "png":
                    share_intent.setType("image/png");
                    break;
                case "jpg":
                    share_intent.setType("image/jpg");
                    break;
                case "webp":
                    share_intent.setType("image/webp");
                    break;
                case "heif":
                    share_intent.setType("image/heif");
                    break;
                case "gif":
                    share_intent.setType("image/gif");
                    break;
                default:
                    share_intent.setType("image/*");
                    break;
            }

            share_intent.setAction(Intent.ACTION_SEND);
            share_intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            share_intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            Log.i("shareImage()", "uri = " + contentUri);
            startActivity(Intent.createChooser(share_intent, "Share"));

        } else {
            Toast.makeText(getApplicationContext(), R.string.output_not_exits, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();

        formats = getResources().getStringArray(R.array.format);
        BUSY = getResources().getString(R.string.busy);
        ERR = getString(R.string.error);
        DONE = getString(R.string.done);

        SharedPreferences mySharePerferences = getSharedPreferences("config", Activity.MODE_PRIVATE);
        tileSize = mySharePerferences.getInt("tileSize", 0);
        threadCount = mySharePerferences.getString("threadCount", "");
        keepScreen = mySharePerferences.getBoolean("keepScreen", false);

        useMultFiles = mySharePerferences.getBoolean("useMultFiles", false);
        prePng = mySharePerferences.getBoolean("PrePng", true);
        preFrame = mySharePerferences.getBoolean("PreFrame", true);
        useCPU = mySharePerferences.getBoolean("useCPU", false);
        autoSave = mySharePerferences.getBoolean("autoSave", false);
        showSearchView = mySharePerferences.getBoolean("showSearchView", false);
        if (showSearchView)
            searchView.setVisibility(View.VISIBLE);
        else
            searchView.setVisibility(View.GONE);

        notify = mySharePerferences.getInt("notify", 0);

        format = mySharePerferences.getInt("format", 0);
        name = mySharePerferences.getInt("name", 0);
        name2 = mySharePerferences.getInt("name2", 0);
        List<String> extraCmd = getExtraCommands(
                mySharePerferences.getString("extraPath", "").trim()
                , mySharePerferences.getString("extraCommand", "").trim()
                , mySharePerferences.getString("classicalFilters", getString(R.string.default_classical_filters)).split("\\s+")
                , mySharePerferences.getString("magickFilters", getString(R.string.default_magick_filters)).split("\\s+")
        );

        if (extraCmd.size() > 0) {
            String[] presetCommand = getResources().getStringArray(R.array.style_array);
            extraCmd.addAll(0, Arrays.asList(presetCommand));
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, extraCmd);
            spinner.setAdapter(adapter);
        }

        spinner.setSelection(selectCommand);

        savePath = mySharePerferences.getString("savePath", "");
        if (savePath.isEmpty())
            savePath = galleryPath;
        try {
            File file = new File(savePath);
            if (file.isFile())
                file.delete();
            if (!file.exists())
                file.mkdirs();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean readFileFromShare() {
        Intent intent = getIntent();
        String action = intent.getAction();

        if (Intent.ACTION_SEND.equals(action)) {
            deleteFile(inputFile);
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            inputFileName = getFileName(uri, this).replaceFirst("\\.[^\\.]+$", "");
            Log.i("input file name", inputFileName);
            whiteFileFromUri(uri, "");

        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> imageUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            handleSelectedImages(imageUris);

        }
        return false;
    }

    private boolean whiteFileFromUri(Uri uri, String path) {
        if (uri != null) {
            try {
                InputStream in = getContentResolver().openInputStream(uri);
                if (null != in)
                    saveInputImage(in, path);
                else
                    Toast.makeText(this, R.string.share_is_null, Toast.LENGTH_SHORT).show();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 生成用户自定义命令.
     */
    private List<String> getExtraCommands(String extraPath, String extraCommand, String[] classicalFilters, String[] magickFilters) {

        List<String> cmdList = new ArrayList<>();
        List<String> cmdLabel = new ArrayList<>();

        String[] classicalResize = {"2", "4", "10"};

        for (String f : classicalFilters) {
            for (String s : classicalResize) {
                cmdList.add("./resize-ncnn -i input.png -o output.png  -m " + f + " -s " + s);
                cmdLabel.add("Classical-" + f + "-x" + s);
            }
        }

        String[] magickResize = {"200%", "400%", "1000%"};

        for (String f : magickFilters) {
            for (String s : magickResize) {
                cmdList.add("./magick input.png -filter " + f + " -resize " + s + " output.png ");
                cmdLabel.add("Magick-" + f + "-x" + s.replaceFirst("(\\d+)00%", "$1"));
            }
        }

        if (!extraPath.isEmpty()) {
            File[] folders = new File(extraPath).listFiles();
            if (folders == null)
                Log.e("getExtraCommands", "extraPath folders is null");
            else {
                Arrays.sort(folders, Comparator.comparing(a -> ((File) a).getName()));
                for (File folder : folders) {
                    String name = folder.getName();
                    if (name.endsWith(".mnn") || name.startsWith("models-MNN")) {
                        if (folder.isDirectory()) {
                            File[] files = folder.listFiles();
                            Arrays.sort(files, Comparator.comparing(a -> ((File) a).getName()));
                            for (File file : files) {
                                if (file.getName().endsWith(".mnn")) {
                                    String[] v = getNameFromModelPath(file.getAbsolutePath(), "MNNSR");
                                    cmdList.add("./mnnsr-ncnn -i input.png -o output.png  -m " + file.getAbsolutePath() + " -s " + v[1]);
                                    cmdLabel.add(v[0]);
                                }
                            }
                        } else {
                            String[] v = getNameFromModelPath(folder.getAbsolutePath(), "MNNSR");
                            cmdList.add("./mnnsr-ncnn -i input.png -o output.png  -m " + folder.getAbsolutePath() + " -s " + v[1]);
                            cmdLabel.add(v[0]);
                        }
                    } else if (folder.isDirectory() && name.startsWith("models")) {
                        String model = name.replace("models-", "");
                        String scaleMatcher = ".*x(\\d+).*";
                        String noiseMatcher = "";
                        String command = "./realsr-ncnn -i input.png -o output.png  -m " + folder.getAbsolutePath() + " -s ";

                        if (name.matches("models-(cugan|cunet|upconv).*")) {
                            model = name.replace("models-", "Waifu2x-");
                            scaleMatcher = ".*scale(\\d+).*";
                            command = "./waifu2x-ncnn -i input.png -o output.png  -m " + folder.getAbsolutePath() + " -s ";
                            noiseMatcher = "noise(\\d+).*";
                        } else if (name.matches("models-srmd.*")) {
                            if (name.equals("models-srmd"))
                                model = "SRMD";
                            else
                                model = name.replace("models-srmd", "SRMD-");
                            command = "./srmd-ncnn -i input.png -o output.png  -m " + folder.getAbsolutePath() + " -s ";
                        } else if (name.startsWith("models-DF2K")) {
                            model = name.replace("models-", "RealSR-");
                        } else if (name.startsWith("models-mnn")) {

                        }

                        List<String> suffix = genCmdFromModel(folder, scaleMatcher, noiseMatcher);
                        for (String s : suffix) {
                            cmdList.add(command + s);
                            cmdLabel.add(model + "-x" + s.replace(" -n ", "-noise"));
                        }
                    }
                }
            }
        }

        int l = command_0.length;
        command = new String[cmdList.size() + l];

        System.arraycopy(command_0, 0, command, 0, l);
        for (int i = 0; i < cmdList.size(); i++)
            command[l + i] = cmdList.get(i);

        if (!extraCommand.isEmpty()) {
            String[] cmds = extraCommand.split("\n");
            cmdLabel.addAll(Arrays.asList(cmds));
        }

        return cmdLabel;
    }

    private static List<String> genCmdFromModel(File folder, String scaleMatcher, String noiseMatcher) {
        List<String> list = new ArrayList<>();
        File[] files = folder.listFiles();

        List<String> names = new ArrayList<>();
        for (File f : files) {
            String name = f.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith("bin"))
                names.add(name);
        }

        String[] fileNames = names.toArray(new String[0]);
        Arrays.sort(fileNames);

        for (String name : fileNames) {
            String s;
            if (name.matches(scaleMatcher))
                s = (name.replaceFirst(scaleMatcher, "$1"));
            else
                s = "1";

            if (!noiseMatcher.isEmpty()) {
                String noise = name.replaceFirst(noiseMatcher, "$1");
                if (noise.matches("\\d+")) {
                    int n = Integer.parseInt(noise);
                    s = s + " -n " + n;
                }
            }
            if (!list.contains(s))
                list.add(s);
        }
        return list;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.photo_view);
        logTextView = findViewById(R.id.tv_log);
        searchView = findViewById(R.id.serarch_view);

        SharedPreferences mySharePerferences = getSharedPreferences("config", Activity.MODE_PRIVATE);
        prePng = mySharePerferences.getBoolean("PrePng", true);
        preFrame = mySharePerferences.getBoolean("PreFrame", true);

        int version = mySharePerferences.getInt("version", 0);
        String defaultCommand = mySharePerferences.getString("defaultCommand", "");
        searchView.setQuery(defaultCommand, false);

        cache_dir = this.getCacheDir().getAbsolutePath();
        AssetsCopyer.releaseAssets(this, "realsr", cache_dir, version == BuildConfig.VERSION_CODE);

        SharedPreferences.Editor editor = mySharePerferences.edit();
        editor.putInt("version", BuildConfig.VERSION_CODE);
        editor.apply();

        int orientation = mySharePerferences.getInt("ORIENTATION", 0);
        if (orientation == 1) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        } else if (orientation == 2)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        else if (orientation == 3) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }

        dir = cache_dir + "/realsr";

        outputFile = new File(dir, "output.png");
        outputGif = new File(dir, "output.gif");
        inputFile = new File(dir, "input.png");
        titleFile = new File(dir, "img/realsr.png");
        showImage(titleFile, getString(R.string.default_log));

        run_command("chmod 777 " + dir + " -R");

        spinner = findViewById(R.id.spinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectCommand = pos;
                Log.i("setOnItemSelectedListener", "select " + pos);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        selectCommand = mySharePerferences.getInt("selectCommand", 2);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                String q = searchView.getQuery().toString().trim();
                if (!run_fake_command(q)) {
                    stopCommand();
                    new Thread(() -> run20(query, false, true)).start();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.trim().length() < 2) {
                    if (menuProgress != null)
                        menuProgress.setTitle("");
                    return true;
                }
                if (imageView.getVisibility() == View.VISIBLE)
                    imageView.setVisibility(View.GONE);
                return true;
            }
        });

        findViewById(R.id.btn_open).setOnClickListener(view -> {
            if (useMultFiles) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                startActivityForResult(intent, SELECT_MULTI_IMAGE);
            } else {
                Intent i = new Intent(Intent.ACTION_PICK);
                i.setType("image/*");
                startActivityForResult(i, SELECT_IMAGE);
            }
        });

        findViewById(R.id.btn_save).setOnClickListener(view -> {
            File f = inputIsGifAnimation ? outputGif : outputFile;
            if (!f.exists()) {
                Toast.makeText(this, R.string.output_not_exits, Toast.LENGTH_SHORT).show();
                return;
            } else if (f.isDirectory()) {
                File[] files = f.listFiles();
                if (files.length < 1) {
                    Toast.makeText(this, R.string.output_not_exits, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.output_is_dir, Toast.LENGTH_SHORT).show();
                }
                return;
            }
            run_command(saveOutputCmd());
            checkSaveOutput();
        });

        findViewById(R.id.btn_run).setOnClickListener(view -> {
            menuProgress.setTitle("");
            {
                stopCommand();
                log = "";
                StringBuffer cmd;

                if (selectCommand >= command.length) {
                    cmd = new StringBuffer(spinner.getSelectedItem().toString());
                    Log.w("btn_run.onClick", "select=" + selectCommand + ", length=" + command.length + " text=" + cmd);
                    if (run_fake_command(cmd.toString()))
                        return;
                } else {
                    cmd = new StringBuffer(command[selectCommand]);
                    if (command[selectCommand].matches("./(realsr|srmd|waifu2x|realcugan|mnnsr)-ncnn.+")) {
                        if (tileSize > 0)
                            cmd.append(" -t ").append(tileSize);
                        if (threadCount.length() > 0)
                            cmd.append(" -j ").append(threadCount);
                        if (useCPU && !cmd.toString().startsWith("./srmd"))
                            cmd.append(" -g -1");
                    }
                }

                deleteFile(outputFile);
                if (inputIsGifAnimation) {
                    outputGif.delete();
                    outputFile.mkdir();
                }
                if (keepScreen) {
                    logTextView.setKeepScreenOn(true);
                }

                new Thread(() -> {
                    if (run20(cmd.toString(), false, true)) {
                        if (inputFile.isDirectory()) {
                            if (inputIsGifAnimation)
                                scanFiles(new String[]{outputSavePath});
                            else {
                                File[] files = inputFile.listFiles();
                                Log.i("befor scanFiles()", "inputFile size=" + files.length);
                                List<String> outputPaths = new ArrayList<>();
                                for (File file : files) {
                                    outputPaths.add(savePath + File.separator + file.getName());
                                }
                                scanFiles(outputPaths.toArray(new String[0]));
                            }
                        }

                        boolean showImgView = (cmd.toString().contains("output.png"));
                        if (showImgView) {
                            if (outputFile.exists() && outputFile.isFile()) {
                                updateImage(dir + "/output.png", String.format("%s\n%s", getString(R.string.hr), log), false);
                            } else if (inputIsGifAnimation && outputFile.exists() && outputFile.isDirectory() && outputFile.listFiles().length > 1) {
                                updateImage(outputFile.listFiles()[0].getPath(), String.format("%s\n%s", getString(R.string.hr), log), false);
                            } else {
                                updateImage(dir + "/input.png", String.format("%s\n%s", getString(R.string.lr), log), false);
                            }
                        }
                        if (!showImgView)
                            runOnUiThread(() -> setPreviewVisibility(false));
                    }
                }).start();
            }
        });

        findViewById(R.id.btn_setting).setOnClickListener(view -> {
            Intent intent = new Intent(this, SettingActivity.class);
            this.startActivity(intent);
            overridePendingTransition(0, android.R.anim.slide_out_right);
        });

        // ---- Add queue UI controls programmatically ----
        createQueueUI();

        requirePremision();

        if (menuProgress != null) menuProgress.setTitle("");
        else initProcess = true;

        readFileFromShare();
    }

    private void createQueueUI() {
        // Get the root layout
        View rootView = findViewById(android.R.id.content);
        if (rootView instanceof LinearLayout) {
            LinearLayout rootLayout = (LinearLayout) rootView;
            // Create container for queue controls
            LinearLayout queueLayout = new LinearLayout(this);
            queueLayout.setOrientation(LinearLayout.VERTICAL);
            queueLayout.setPadding(16, 16, 16, 16);
            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(0, 16, 0, 0);
            queueLayout.setLayoutParams(layoutParams);

            // Status text
            queueStatusText = new TextView(this);
            queueStatusText.setText("Queue: 0/0");
            queueStatusText.setTextSize(16);
            queueLayout.addView(queueStatusText);

            // Button row
            LinearLayout buttonRow = new LinearLayout(this);
            buttonRow.setOrientation(LinearLayout.HORIZONTAL);
            buttonRow.setPadding(0, 8, 0, 8);
            queueLayout.addView(buttonRow);

            btnStartQueue = new Button(this);
            btnStartQueue.setText("Start");
            btnStartQueue.setEnabled(false);
            btnStartQueue.setOnClickListener(v -> startQueueProcessing());
            buttonRow.addView(btnStartQueue);

            btnStopQueue = new Button(this);
            btnStopQueue.setText("Stop");
            btnStopQueue.setEnabled(false);
            btnStopQueue.setOnClickListener(v -> stopQueueProcessing());
            buttonRow.addView(btnStopQueue);

            btnClearQueue = new Button(this);
            btnClearQueue.setText("Clear Queue");
            btnClearQueue.setEnabled(false);
            btnClearQueue.setOnClickListener(v -> stopQueueProcessing());
            buttonRow.addView(btnClearQueue);

            btnTogglePreview = new Button(this);
            btnTogglePreview.setText("Hide Preview");
            btnTogglePreview.setOnClickListener(v -> togglePreview());
            buttonRow.addView(btnTogglePreview);

            // Add to root layout
            rootLayout.addView(queueLayout);
        } else {
            Log.e("createQueueUI", "Root layout is not LinearLayout, cannot add queue UI");
        }
    }

    private void startQueueProcessing() {
        if (autoQueue.isEmpty() || queueRunning) return;
        queueRunning = true;
        autoQueueIndex = 0;
        updateQueueUI();
        // Start foreground service
        Intent serviceIntent = new Intent(this, QueueForegroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        processNextInAutoQueue();
    }

    private void stopQueueProcessing() {
        queueRunning = false;
        autoQueue.clear();
        autoQueueIndex = 0;
        stopCommand();
        stopQueueService();
        updateQueueUI();
    }

    private void stopQueueService() {
        Intent stopIntent = new Intent(this, QueueForegroundService.class);
        stopIntent.setAction(QueueForegroundService.ACTION_STOP);
        startService(stopIntent);
    }

    private void updateQueueUI() {
        if (queueStatusText == null) return;
        if (autoQueue.isEmpty()) {
            queueStatusText.setText("Queue: 0/0");
            if (btnStartQueue != null) btnStartQueue.setEnabled(false);
            if (btnStopQueue != null) btnStopQueue.setEnabled(false);
            if (btnClearQueue != null) btnClearQueue.setEnabled(false);
        } else {
            if (queueRunning) {
                queueStatusText.setText("Processing: " + (autoQueueIndex + 1) + "/" + autoQueue.size());
                if (btnStartQueue != null) btnStartQueue.setEnabled(false);
                if (btnStopQueue != null) btnStopQueue.setEnabled(true);
                if (btnClearQueue != null) btnClearQueue.setEnabled(true);
            } else {
                queueStatusText.setText("Queue: " + autoQueue.size() + " images");
                if (btnStartQueue != null) btnStartQueue.setEnabled(true);
                if (btnStopQueue != null) btnStopQueue.setEnabled(false);
                if (btnClearQueue != null) btnClearQueue.setEnabled(true);
            }
        }
    }

    private void togglePreview() {
        showPreview = !showPreview;
        if (showPreview) {
            if (currentPreviewFile != null && currentPreviewFile.exists()) {
                setPreviewVisibility(true);
            }
            btnTogglePreview.setText("Hide Preview");
        } else {
            setPreviewVisibility(false);
            btnTogglePreview.setText("Show Preview");
        }
    }

    private void setPreviewVisibility(boolean visible) {
        if (visible && showPreview && currentPreviewFile != null && currentPreviewFile.exists()) {
            imageView.setVisibility(View.VISIBLE);
            imageView.setImage(ImageSource.uri(currentPreviewFile.getAbsolutePath()));
        } else {
            imageView.setVisibility(View.GONE);
        }
    }

    // Modified updateImage to use centralized visibility and respect showPreview
    private void updateImage(final String path, String text, boolean keepScreen) {
        Log.i("saveInputImage", "runOnUiThread");
        File file = new File(path);
        runOnUiThread(() -> {
            if (file.exists()) {
                if (file.isDirectory()) {
                    if (file.listFiles().length > 0) {
                        currentPreviewFile = file.listFiles()[0];
                        setPreviewVisibility(true);
                        Log.i("saveInputImage", "finish, directory");
                    } else {
                        currentPreviewFile = null;
                        setPreviewVisibility(false);
                        Log.i("saveInputImage", "finish, empty directory");
                    }
                    logTextView.setText(text);
                } else {
                    currentPreviewFile = file;
                    setPreviewVisibility(true);
                    logTextView.setText(getImageResolation(file, text));
                    Log.i("saveInputImage", "finish, file");
                }
            } else {
                currentPreviewFile = null;
                setPreviewVisibility(false);
                Log.i("saveInputImage", "skip");
            }
            if (keepScreen) {
                logTextView.setKeepScreenOn(false);
            }
        });
    }

    // Modified showImage to store currentPreviewFile and respect showPreview
    private void showImage(File file, String info) {
        if (file == null) {
            currentPreviewFile = null;
            setPreviewVisibility(false);
            logTextView.setText(info);
        } else if (file.exists() && (!file.isDirectory())) {
            currentPreviewFile = file;
            setPreviewVisibility(true);
            logTextView.setText(getImageResolation(file, info));
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files.length < 1) {
                currentPreviewFile = null;
                setPreviewVisibility(false);
                logTextView.setText(getString(R.string.image_not_exists));
            } else {
                currentPreviewFile = files[0];
                setPreviewVisibility(true);
                logTextView.setText(getString(R.string.image_is_directory));
            }
        } else {
            currentPreviewFile = null;
            setPreviewVisibility(false);
            logTextView.setText(getString(R.string.image_not_exists));
        }
    }

    // Modified onActivityResult for multi-select to not auto-start
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK && null != data) {
            Uri url = data.getData();

            if (requestCode == SELECT_IMAGE && null != url) {
                deleteFile(inputFile);
                inputFileName = getFileName(url, this).replaceFirst("\\.[^\\.]+$", "");
                Log.i("input file name", inputFileName);
                InputStream in;
                try {
                    in = getContentResolver().openInputStream(url);
                    if (null != in)
                        saveInputImage(in, "");
                    else
                        Toast.makeText(this, "input == null", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            } else if (requestCode == SELECT_MULTI_IMAGE) {
                List<Uri> imageUris = new ArrayList<>();
                ClipData clipData = data.getClipData();
                if (clipData != null) {
                    for (int i = 0; i < clipData.getItemCount(); i++) {
                        imageUris.add(clipData.getItemAt(i).getUri());
                    }
                }
                // Replace queue with new selection
                autoQueue.clear();
                autoQueue.addAll(imageUris);
                autoQueueIndex = 0;
                queueRunning = false;
                updateQueueUI();
                // Do not start processing automatically
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // Modified processNextInAutoQueue to update status and handle completion
    private void processNextInAutoQueue() {
        if (!queueRunning || autoQueueIndex >= autoQueue.size()) {
            // Queue finished
            queueRunning = false;
            autoQueue.clear();
            stopQueueService();
            updateQueueUI();
            return;
        }
        updateQueueUI();
        Uri uri = autoQueue.get(autoQueueIndex);
        deleteFile(inputFile);
        String name = getFileName(uri, this);
        inputFileName = (name != null) ? name.replaceFirst("\\.[^\\.]+$", "") : "";
        Log.i("processNextInAutoQueue", "input file name=" + inputFileName + " index=" + autoQueueIndex);
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (null != in) {
                saveInputImage(in, "");
                runOnUiThread(this::triggerRunClick);
            } else {
                Toast.makeText(this, "input == null", Toast.LENGTH_SHORT).show();
                autoQueueIndex++;
                processNextInAutoQueue();
            }
        } catch (Exception e) {
            e.printStackTrace();
            autoQueueIndex++;
            processNextInAutoQueue();
        }
    }

    private void triggerRunClick() {
        View runBtn = findViewById(R.id.btn_run);
        if (runBtn != null)
            runBtn.performClick();
    }

    // rest of existing methods unchanged (run20, get_gif_frame_delay, run_command, etc.)
    // ... [keeping the same as previous version but with the modifications below]

    public synchronized boolean run20(@NonNull String cmd, boolean bench_mark_mode, boolean sr) {
        newTask = false;
        Log.i("run20", "cmd = " + cmd);
        final long timeStart = System.currentTimeMillis();
        boolean export_dir = false;

        if (cmd.startsWith("./realsr-ncnn")
                || cmd.startsWith("./mnnsr-ncnn")
                || cmd.startsWith("./srmd-ncnn")
                || cmd.startsWith("./realcugan-ncnn")
                || cmd.startsWith("./resize-ncnn")
                || cmd.startsWith("./waifu2x-ncnn")
                || cmd.startsWith("./magick input")
                || cmd.startsWith("./Anime4k")
        ) {
            if (cmd.contains(" input.png ") && cmd.contains(" output.png")) {
                if (inputFile.isDirectory() && !inputIsGifAnimation) {
                    export_dir = true;
                    cmd = cmd.replace(" output.png ", " '" + savePath + "' ");
                }

                if (cmd.startsWith("./magick input.png") || cmd.startsWith("./resize-ncnn -i input.png")) {
                    Log.i("run20", "deleteFile " + outputFile);
                    deleteFile(outputFile);
                }
            }

            runOnUiThread(() -> {
                menuProgress.setTitle(BUSY);
                sendNotification(this, BUSY);
            });
            modelName = "Real-ESRGAN-anime";
            if (cmd.matches(".+\\s-m(\\s+)[^\\s]*models-.+")) {
                modelName = cmd.replaceFirst(".+\\s-m(\\s+)[^\\s]*models-([^\\s]+).*", "$2");
            }
            if (cmd.startsWith("./Anime4k")) {
                modelName = "Anime4k";
                if (cmd.contains("-w"))
                    modelName += "-ACNet";
                if (cmd.contains("-H"))
                    modelName += "-HDN";
            } else if (modelName.matches("(se|nose|pro)")) {
                modelName = "Real-CUGAN-" + modelName;
            } else if (cmd.startsWith("./realcugan-ncnn")) {
                modelName = "Real-CUGAN";
                if (cmd.contains(" -c "))
                    modelName += cmd.replaceFirst(".+\\s-c(\\s+)([^\\s]+)\\s.*", "-C$2");
                if (cmd.contains(" -n "))
                    modelName += cmd.replaceFirst(".+\\s-n(\\s+)([^\\s]+)\\s.*", "-Noise$2");
            } else if (cmd.matches(".+\\s-m(\\s+)(bicubic|bilinear|nearest|avir|de-nearest).*")) {
                modelName = cmd.replaceFirst(".+\\s-m(\\s+)(bicubic|bilinear|nearest|lancir|avir|de-nearest).*", "Classical-$2");
            } else if (cmd.matches(".*waifu2x.+models-(cugan|cunet|upconv).*")) {
                modelName = cmd.replaceFirst(".*waifu2x.+models-(cugan|cunet|upconv_7_photo|upconv_7_anime).*", "Waifu2x-$1");
            } else if (cmd.startsWith("./magick input")) {
                if (cmd.contains("-filter"))
                    modelName = cmd.replaceFirst(".*-filter\\s+(\\w+).+", "Magick-$1");
                else
                    modelName = "Magick";
            } else if (cmd.startsWith("./mnnsr")) {
                if (cmd.matches(".+\\s-d\\s+\\d+\\s.*")) {
                    modelName = "MNNSR-Decensor" + cmd.replaceFirst(".+\\s-d\\s+(\\d+)\\s.*", "$1");
                } else {
                    String[] v = getNameFromModelPath(cmd.replaceFirst(".+\\s-m(\\s+)([^\\s]+)\\s.*", "$2"), "MNNSR");
                    modelName = v[0];
                }
            }
        } else
            modelName = "SR";

        StringBuilder result = new StringBuilder();
        HashSet<String> results = new HashSet<>();
        boolean result_fail = false;

        final boolean run_ncnn = bench_mark_mode || !modelName.equals("SR");
        boolean export_one_file = run_ncnn && (autoSave || (inputFile.isDirectory() && inputIsGifAnimation)) && cmd.contains("output.png");
        if (bench_mark_mode) {
            export_one_file = false;
            runOnUiThread(() -> {
                menuProgress.setTitle(BUSY);
                sendNotification(this, BUSY);
            });
        }
        final boolean save = export_one_file;

        try {
            ProcessBuilder processBuilder = new ProcessBuilder("sh");
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        OutputStream os = process.getOutputStream();

        try {
            os.flush();
            os.write(("cd " + dir + "; chmod 777 *ncnn; export LD_LIBRARY_PATH=" + dir + "\n").getBytes());
            os.flush();

            if (save) {
                String export_cmd = saveOutputCmd();
                if (inputIsGifAnimation)
                    cmd = cmd + ";./magick -delay " + inputGifDelay + " output.png/* -loop 0 '" + outputSavePath + "'";
                else
                    cmd = cmd + ";" + export_cmd;
            } else {
                outputSavePath = "";
            }

            Log.i("run20", "write cmd start; final cmd: " + cmd + " [end]");
            os.write((cmd + "\n").getBytes());
            os.flush();

            Log.i("run20", "write cmd finish");
            os.write("exit\n".getBytes());
            os.flush();
            os.close();

            String line;
            Log.i("run20", "process.getErrorStream() start");

            try {
                while ((line = outputReader.readLine()) != null) {
                    if (line.contains("unused DT entry"))
                        continue;

                    Log.d("run20 errorResult", line);

                    boolean p = run_ncnn && line.matches("\\s*\\d([0-9.]*)%(\\s.+)?");
                    progressText = line.trim().split("\\s")[0];

                    if (!p) {
                        if (line.contains("vkQueueSubmit") || line.endsWith(" fault") || line.startsWith("Killed")) {
                            result_fail = true;
                        }

                        if (line.equals("save result...") || line.equals("busy...") || line.equals("check result...")) {

                        } else if (bench_mark_mode && results.contains(line)) {
                            line = "";
                        } else {
                            if (bench_mark_mode)
                                results.add(line);
                            result.append(line).append("\n");
                            line = "";
                        }
                    }

                    String finalLine = line;
                    runOnUiThread(() -> {
                        logTextView.setText(result + finalLine);
                        if (p) {
                            menuProgress.setTitle(progressText);
                            sendNotification(this, finalLine);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    outputReader.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            Log.i("run20", "process output stream finish");
        } catch (Exception e) {
            e.printStackTrace();
            sendNotification(this, ERR);
            return false;
        }
        Log.d("run_20", "finish, process " + (process != null));

        try {
            Log.d("run_20", "finish, exitValue " + process.exitValue());
            if (process.exitValue() != 0) process.destroy();
        } catch (Exception e) {
        }

        if (newTask || process == null) {
            log = result.append("\nbreak").toString();
            runOnUiThread(() -> {
                logTextView.setText(log);
                menuProgress.setTitle("");
            });
            return false;
        }

        if (result_fail)
            result.append("\nfail, use ").append((float) (System.currentTimeMillis() - timeStart) / 1000).append(" second");
        else
            result.append("\nfinish, use ").append((float) (System.currentTimeMillis() - timeStart) / 1000).append(" second");

        if (bench_mark_mode) {
            result.append(String.format(", Benchmark run on %s\n%s", DeviceInfo.getConfigStr(useCPU, tileSize), DeviceInfo.getInfo(this)));
            Log.i("run20 finish", "Benchmark, ..." + result.substring(Math.max(result.length() - 100, 0)));
        } else if (run_ncnn) {
            result.append(", ").append(modelName + "\n");
            Log.i("run20 finish'", "run_ncnn=" + run_ncnn + ", modelName=" + modelName + ", ..." + result.substring(Math.max(result.length() - 100, 0)));
        } else Log.i("run20 finish", "run_ncnn=false");

        boolean final_export_dir = export_dir;
        boolean final_result_fail = result_fail;
        log = result.toString();
        runOnUiThread(() -> {
            logTextView.setText(log);
            menuProgress.setTitle(DONE);
            sendNotification(this, DONE);

            if (save) {
                if (!outputFile.exists()) {
                    Toast.makeText(getApplicationContext(), R.string.output_not_exits, Toast.LENGTH_SHORT).show();
                } else {
                    checkSaveOutput();
                }
            } else if (final_export_dir) {
                Toast.makeText(getApplicationContext(), R.string.save_succeed, Toast.LENGTH_SHORT).show();
            }
        });

        // Queue advancement outside UI thread
        if (!final_result_fail && queueRunning && !autoQueue.isEmpty() && autoQueueIndex < autoQueue.size()) {
            autoQueueIndex++;
            processNextInAutoQueue();
        } else if (queueRunning) {
            // Failure or no more items: stop queue
            queueRunning = false;
            autoQueue.clear();
            stopQueueService();
            runOnUiThread(this::updateQueueUI);
        }

        Log.i("run20", "finish");
        return true;
    }

    // Inner foreground service class (unchanged)
    public static class QueueForegroundService extends Service {
        private static final String CHANNEL_ID = "queue_processing";
        public static final String ACTION_STOP = "stop";

        @Override
        public void onCreate() {
            super.onCreate();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Queue Processing",
                        NotificationManager.IMPORTANCE_LOW);
                NotificationManager manager = getSystemService(NotificationManager.class);
                manager.createNotificationChannel(channel);
            }

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Processing images")
                    .setContentText("RealSR is processing your queue…")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .build();

            startForeground(1, notification);
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            if (intent != null && ACTION_STOP.equals(intent.getAction())) {
                stopForeground(true);
                stopSelf();
            }
            return START_STICKY;
        }

        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up queue and service when activity is destroyed
        if (queueRunning) {
            stopQueueProcessing();
        }
    }
}
