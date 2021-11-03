package com.mister_chan.chaoxinganswersearcher;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.ValueCallback;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private class QuestionBank {
        private String url;
        private String exception;

        QuestionBank(String url, String exception) {
            this.url = url;
            this.exception = exception;
        }
    }

    private class SearchingAnswerAsyncTask extends AsyncTask<Void, Void, Void> {

        private Button bAnswer, bSearch;
        private LinearLayout llAnswers;
        private String number;
        private String question;

        SearchingAnswerAsyncTask(String number, String question, View parent) {
            this.number = number;
            this.question = question;
            bAnswer = parent.findViewById(R.id.b_answer);
            bSearch = parent.findViewById(R.id.b_search);
            llAnswers = parent.findViewById(R.id.ll_answers);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            for (QuestionBank qb : questionBanks) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String answer = getJsonObject(qb.url + question).getString("answer");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView tv = new TextView(MainActivity.this);
                                    tv.setText(answer);
                                    llAnswers.addView(tv);
                                    bSearch.setVisibility(View.GONE);
                                    bAnswer.setVisibility(View.VISIBLE);
                                }
                            });
                        } catch (IOException | JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
            return null;
        }
    }

    private ScrollView svQuestions;
    private QuestionBank[] questionBanks = {
            new QuestionBank("http://q.zhizhuoshuma.cn/?question=", "该题目目前没有收录，换个题目吧！"),
            new QuestionBank("http://monkeytools.cn/api/query?question=", "暂时查询不到"),
            new QuestionBank("http://api.gochati.cn/jsapi.php?token=test123&q=", ""),
            new QuestionBank("http://immu.52king.cn/api/wk/index.php?c=", ""),
            new QuestionBank("http://api.902000.xyz:88/wkapi.php?q=", "")
    };
    private LayoutInflater layoutInflater;
    private LinearLayout llQuestions;
    private WebView webView;

    public void answer(View v) {

    }

    public void copyQuestion(View v) {
        ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Label", ((TextView) v).getText()));
        Toast.makeText(MainActivity.this, "已复制题目", Toast.LENGTH_SHORT).show();
    }

    JSONObject getJsonObject(String urlString) throws IOException, JSONException {
        HttpURLConnection httpURLConnection = null;
        URL url = new URL(urlString);
        httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setRequestMethod("GET");
        httpURLConnection.setReadTimeout(10000);
        httpURLConnection.setConnectTimeout(15000);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.connect();

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
        StringBuilder stringBuilder = new StringBuilder();

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line + "\n");
        }
        bufferedReader.close();

        return new JSONObject(stringBuilder.toString());
    }

    public void match() {
        llQuestions.removeAllViews();
        webView.evaluateJavascript("" +
                        "var elements = document.getElementsByClassName(\"mark_name colorDeep\"), questions = [];" +
                        "for (var i = 0; i < elements.length; i++) {" +
                        "    questions.push(elements[i].textContent.match(/(?<=\\d+\\. \\(.+\\) ).+/)[0]);" +
                        "}" +
                        "questions.join(\",,\")",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (!"null".equals(value)) {
                            String[] questions = value.split(",,");
                            for (int i = 0; i < questions.length; i++) {
                                LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout.question, null);
                                ((TextView) layout.findViewById(R.id.tv_number)).setText(String.valueOf(i + 1));
                                ((TextView) layout.findViewById(R.id.tv_question)).setText(questions[i]);
                                llQuestions.addView(layout.findViewById(R.id.ll_question));
                            }
                        }
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        layoutInflater = LayoutInflater.from(this);
        llQuestions = findViewById(R.id.ll_questions);
        svQuestions = findViewById(R.id.sv_questions);

        findViewById(R.id.b_questions).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (svQuestions.getVisibility() == View.VISIBLE) {
                    svQuestions.setVisibility(View.GONE);
                } else {
                    svQuestions.setVisibility(View.VISIBLE);
                }
            }
        });

        webView = findViewById(R.id.wv);
        WebSettings ws = webView.getSettings();
        ws.setAllowFileAccess(true);
        ws.setAppCacheEnabled(true);
        ws.setBlockNetworkImage(true);
        ws.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        ws.setDatabaseEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setJavaScriptEnabled(true);
        ws.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webView.getSettings().setBlockNetworkImage(false);
                if (url.startsWith("https://mooc1.chaoxing.com/mooc2/work/dowork?courseId=")) {
                    match();
                }
                super.onPageFinished(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                webView.getSettings().setBlockNetworkImage(true);
                super.onPageStarted(view, url, favicon);
            }
        });

        webView.loadUrl("http://www.chaoxing.com/");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
        }
        return super.onKeyDown(keyCode, event);
    }

    public void search(View v) {
        v.setEnabled(false);
        ((Button) v).setText("请稍等");
        View parent = (View) v.getParent().getParent();
        String number = ((TextView) parent.findViewById(R.id.tv_number)).getText().toString();
        String question = ((TextView) parent.findViewById(R.id.tv_question)).getText().toString();
        new SearchingAnswerAsyncTask(number, question, parent).execute();
    }
}