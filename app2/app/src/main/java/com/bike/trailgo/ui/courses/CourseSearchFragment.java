package com.bike.trailgo.ui.courses;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bike.trailgo.MainViewModel;
import com.bike.trailgo.R;
import com.bike.trailgo.net.Server;
import com.bike.trailgo.ui.BaseFragment;

public class CourseSearchFragment extends BaseFragment {
    WebView webview;
    private static final String ENTRY_URL = Server.getUrl("app/riding/CourseSearch");
    String LoginId, Nickname, r_image;
    ImageView userImage;
    SharedPreferences auto;
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_course_search, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        model = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        controller = Navigation.findNavController(view);


        webview = (WebView) view.findViewById(R.id.searchMap);
        webview.getSettings().setJavaScriptEnabled(true);//자바스크립트 허용
        webview.setWebChromeClient(new WebChromeClient());
        webview.getSettings().setLoadWithOverviewMode(true);

        webview.setWebViewClient(new WebViewClient());


        webview.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                // 여기서 WebView의 데이터를 가져오는 작업을 한다.
                if (url.equals(ENTRY_URL)) {
                    String keyword = LoginId;

                    String script = "javascript:function afterLoad() {"
                            + "document.getElementById('userid').value = '" + keyword + " "
                            + "};"
                            + "afterLoad();";

                    view.loadUrl(script);
                }
            }

        });
        webview.loadUrl(ENTRY_URL);

    }
}
