package com.bike.trailgo.ui.member;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bike.trailgo.MainActivity;
import com.bike.trailgo.R;
import com.bike.trailgo.data.LocalRideStore;
import com.bike.trailgo.dto.AuthSession;
import com.bike.trailgo.net.Server;
import com.bike.trailgo.setting.AccountSession;
import com.bike.trailgo.setting.PrivacyCenterActivity;
import com.kakao.sdk.auth.model.OAuthToken;
import com.kakao.sdk.common.model.ClientError;
import com.kakao.sdk.common.model.ClientErrorCause;
import com.kakao.sdk.user.UserApiClient;
import com.kakao.sdk.user.model.User;

import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * First screen: continue with Kakao or Naver, or start without an account (local-only guest mode).
 * Naver is a placeholder; Kakao delegates authentication to the official Kakao SDK.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TERMS_TEXT = "서비스 이용약관";
    private static final String PRIVACY_TEXT = "개인정보 처리방침";

    private boolean signingIn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (AccountSession.isMember(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_login);
        drawBehindSystemBars();
        findViewById(R.id.login_kakao).setOnClickListener(v -> startKakaoLogin());
        findViewById(R.id.login_naver).setOnClickListener(v -> notReady("네이버"));
        findViewById(R.id.guest_start).setOnClickListener(v -> {
            AccountSession.beginGuest(this);
            openMainScreen();
        });

        bindTerms((TextView) findViewById(R.id.login_terms));
    }

    /** Lets the picture run under the status and navigation bars; the content keeps clear of them. */
    private void drawBehindSystemBars() {
        View root = findViewById(R.id.login_root);
        int flags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        root.setSystemUiVisibility(flags);

        final View content = findViewById(R.id.login_content);
        final View panel = findViewById(R.id.login_panel);
        final int panelBottom = panel.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            content.setPadding(content.getPaddingLeft(), insets.getSystemWindowInsetTop(),
                    content.getPaddingRight(), content.getPaddingBottom());
            panel.setPadding(panel.getPaddingLeft(), panel.getPaddingTop(), panel.getPaddingRight(),
                    panelBottom + insets.getSystemWindowInsetBottom());
            return insets;
        });
        root.requestApplyInsets();
    }

    private void notReady(String provider) {
        Toast.makeText(this, provider + " 로그인은 아직 준비 중입니다.", Toast.LENGTH_SHORT).show();
    }

    private void startKakaoLogin() {
        Function2<OAuthToken, Throwable, Unit> callback = (token, error) -> {
            if (error != null) {
                Toast.makeText(this, "카카오 로그인에 실패했습니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show();
            } else if (token != null) {
                signInToServer(token.getAccessToken());
            }
            return Unit.INSTANCE;
        };

        if (UserApiClient.getInstance().isKakaoTalkLoginAvailable(this)) {
            UserApiClient.getInstance().loginWithKakaoTalk(this, (token, error) -> {
                if (error != null) {
                    // Do not open a browser after the user intentionally closes KakaoTalk.
                    if (error instanceof ClientError
                            && ((ClientError) error).getReason() == ClientErrorCause.Cancelled) {
                        return Unit.INSTANCE;
                    }
                    UserApiClient.getInstance().loginWithKakaoAccount(this, callback);
                } else if (token != null) {
                    signInToServer(token.getAccessToken());
                }
                return Unit.INSTANCE;
            });
        } else {
            UserApiClient.getInstance().loginWithKakaoAccount(this, callback);
        }
    }

    /**
     * The Kakao token is not trusted by this app: the server checks it with Kakao and answers with its
     * own session, which is what every later call is authorised by.
     */
    private void signInToServer(String kakaoAccessToken) {
        if (signingIn) {
            return;
        }
        signingIn = true;
        Server.getInstance().getApi().signInWithKakao(kakaoAccessToken).enqueue(new Callback<AuthSession>() {
            @Override
            public void onResponse(Call<AuthSession> call, Response<AuthSession> response) {
                signingIn = false;
                AuthSession session = response.body();
                if (response.isSuccessful() && session != null && session.isComplete()) {
                    AccountSession.completeSocialLogin(LoginActivity.this, session);
                    if (AccountSession.isMember(LoginActivity.this)) {
                        openMainScreen();
                    } else {
                        toast("이 기기에서는 로그인 정보를 안전하게 저장할 수 없습니다.");
                    }
                } else if (response.code() == 503) {
                    toast("로그인 서버를 사용할 수 없습니다. 잠시 후 다시 시도해 주세요.");
                } else {
                    toast("카카오 로그인에 실패했습니다. 다시 시도해 주세요.");
                }
            }

            @Override
            public void onFailure(Call<AuthSession> call, Throwable t) {
                signingIn = false;
                toast("서버와 통신하지 못했습니다. 네트워크를 확인해 주세요.");
            }
        });
    }

    private void toast(String message) {
        if (!isFinishing() && !isDestroyed()) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }
    private void bindTerms(TextView view) {
        String full = "계속하면 " + TERMS_TEXT + " 및 " + PRIVACY_TEXT + "에 동의하게 됩니다.";
        SpannableString text = new SpannableString(full);
        link(text, full, TERMS_TEXT, () -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Server.getUrl("terms"))));
            } catch (RuntimeException e) {
                Toast.makeText(this, "이용약관을 열 수 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
        link(text, full, PRIVACY_TEXT, () -> startActivity(new Intent(this, PrivacyCenterActivity.class)));
        view.setText(text);
        view.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private static void link(SpannableString text, String full, String target, final Runnable action) {
        int start = full.indexOf(target);
        if (start < 0) {
            return;
        }
        text.setSpan(new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                action.run();
            }

            @Override
            public void updateDrawState(@NonNull TextPaint paint) {
                paint.setColor(0xFF168A45);
                paint.setUnderlineText(true);
            }
        }, start, start + target.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private void openMainScreen() {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        startActivity(new Intent(this, MainActivity.class));
        if (AccountSession.isMember(this) && LocalRideStore.hasRecords(this)) {
            Toast.makeText(this, "이 기기에 게스트 주행 기록이 있습니다. 서버 동기화는 아직 자동으로 진행되지 않습니다.", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
