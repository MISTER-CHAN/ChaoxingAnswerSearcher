package com.mister_chan.chaoxinganswersearcher;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
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
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static class QuestionBank {
        private final String url;
        private final String exception;

        QuestionBank(String url, String exception) {
            this.url = url;
            this.exception = exception;
        }
    }

    @SuppressLint("StaticFieldLeak")
    private class SearchingAnswerAsyncTask extends AsyncTask<Void, Void, Void> {

        private final Button bSearch;
        private final LinearLayout llAnswers;
        private final String number;
        private final String question;

        SearchingAnswerAsyncTask(String number, String question, View parent) {
            this.number = number;
            this.question = question;
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
                                    tv.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            answer(v);
                                        }
                                    });
                                    llAnswers.addView(tv);
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

    private static final String DO_WORK_URL_PREFIX = "https://mooc1.chaoxing.com/mooc2/work/dowork?courseId=";

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
        String answer = ((TextView) v).getText().toString();
        int number = Integer.parseInt(((TextView) ((LinearLayout) v.getParent().getParent()).findViewById(R.id.tv_number)).getText().toString());
        webView.loadUrl("javascript:" +
                "(function () {" +
                "    var questionLi = document.getElementsByClassName(\"marBom50 questionLi\")[" + (number - 1) + "];" +
                "    switch (questionLi.getAttribute(\"typename\")) {" +
                "        case \"单选题\":" +
                "            var answerBgs = questionLi.getElementsByClassName(\"clearfix answerBg\");" +
                "            for (var i = 0; i < answerBgs.length; i++) {" +
                "                if (answerBgs[i].getElementsByClassName(\"fl answer_p\")[0].innerText == \"" + answer + "\") {" +
                "                    answerBgs[i].click();" +
                "                    break;" +
                "                }" +
                "            }" +
                "            break;" +
                "        case \"多选题\":" +
                "            var answerBgs = questionLi.getElementsByClassName(\"clearfix answerBg\");" +
                "            for (var i = 0; i < answerBgs.length; i++) {" +
                "                if (\"" + answer + "\".includes(answerBgs[i].getElementsByClassName(\"fl answer_p\")[0].innerText)) {" +
                "                    answerBgs[i].click();" +
                "                }" +
                "            }" +
                "            break;" +
                "        case \"判断题\":" +
                "            var answerBgs = questionLi.getElementsByClassName(\"clearfix answerBg\");" +
                "            if ([\"√\", \"对\", \"正确\"].includes(\"" + answer + "\")) {" +
                "                answerBgs[0].click();" +
                "            } else if ([\"×\", \"错\", \"错误\"].includes(\"" + answer + "\")) {" +
                "                answerBgs[1].click();" +
                "            }" +
                "            break;" +
                "        case \"填空题\":" +
                "            questionLi.getElementsByClassName(\"edui-editor-iframeholder edui-default\")[0].lastChild.contentDocument.getElementsByClassName(\"view\")[1].innerText = \"" + answer + "\";" +
                "            break;" +
                "    }" +
                "})()");
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
            stringBuilder.append(line).append("\n");
        }
        bufferedReader.close();

        return new JSONObject(stringBuilder.toString());
    }

    private boolean isPageHeaderEqualTo0(String url) {
        return Pattern.compile("^https://mooc2-anschaoxing\\.com/mycourse/stu\\?courseid=.+&pageHeader=0").matcher(url).find();
    }

    private void matchTask() {
        webView.evaluateJavascript("" +
                        "var elements = document.getElementsByClassName(\"topic-txt fs15\"), questions = [];" +
                        "for (var i = 0; i < elements.length; i++) {" +
                        "    questions.push(elements[i].innerText);" +
                        "}" +
                        "question.join(\",,\")",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        if (!"null".equals(value)) {
                            String[] questions = value.substring(1, value.length() - 1).split(",,");
                            showQuestions(questions);
                        }
                    }
                });
    }

    private void matchWork() {
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
                            String[] questions = value.substring(1, value.length() - 1).split(",,");
                            showQuestions(questions);
                        }
                    }
                });
    }

    @SuppressLint("SetJavaScriptEnabled")
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
        ws.setAppCacheEnabled(false);
        ws.setBlockNetworkImage(true);
        ws.setCacheMode(WebSettings.LOAD_NO_CACHE);
        ws.setDatabaseEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setJavaScriptEnabled(true);
        ws.setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                webView.getSettings().setBlockNetworkImage(false);
                if (url.startsWith(DO_WORK_URL_PREFIX)) {
                    matchWork();
                }
                super.onPageFinished(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                webView.getSettings().setBlockNetworkImage(true);
                if (!url.startsWith(DO_WORK_URL_PREFIX)) {
                    llQuestions.removeAllViews();
                }
                super.onPageStarted(view, url, favicon);
            }
        });

        webView.loadUrl("http://www.chaoxing.com/");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    public void search(View v) {
        v.setEnabled(false);
        View parent = (View) v.getParent().getParent();
        String number = ((TextView) parent.findViewById(R.id.tv_number)).getText().toString();
        String question = ((TextView) parent.findViewById(R.id.tv_question)).getText().toString();
        new SearchingAnswerAsyncTask(number, question, parent).execute();
    }

    private void showQuestions(String[] questions) {
        for (int i = 0; i < questions.length; i++) {
            LinearLayout layout = (LinearLayout) layoutInflater.inflate(R.layout.question, null);
            ((TextView) layout.findViewById(R.id.tv_number)).setText(String.valueOf(i + 1));
            ((TextView) layout.findViewById(R.id.tv_question)).setText(questions[i]);
            llQuestions.addView(layout.findViewById(R.id.ll_question));
        }
    }
}