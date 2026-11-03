package com.bike.trailgo.setting;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bike.trailgo.BuildConfig;

/**
 * In-app privacy notice.  A final, public policy URL can replace this draft through
 * TRAILGO_PRIVACY_POLICY_URL without changing the app code.
 */
public class PrivacyCenterActivity extends AppCompatActivity {
    private static final int GREEN = Color.rgb(46, 125, 50);
    private static final int TEXT = Color.rgb(20, 27, 23);
    private static final int MUTED = Color.rgb(92, 102, 96);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("개인정보");
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        ScrollView scrollView = new ScrollView(this);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(22), dp(20), dp(32));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scrollView);

        addTitle(content, "개인정보 및 계정 관리");
        addBody(content, "이 화면은 현재 TrailGo 앱 코드 기준의 개인정보·위치정보 안내입니다. "
                + "출시 전에는 운영 주체, 문의처, 보관 기간과 처리위탁 정보를 포함한 최종 개인정보처리방침을 공개해야 합니다.");

        addSection(content, "위치를 사용하는 이유");
        addBody(content, "주행을 시작한 뒤 위치 권한을 허용하면 거리, 속도, 경로, 고도와 주행 구간을 계산합니다. "
                + "주행을 저장하면 GPX 경로 파일과 위치 정보가 기록에 포함될 수 있습니다. 백그라운드 주행 기록 중에는 화면이 꺼진 상태에서도 위치를 사용합니다.");

        addSection(content, "처리하는 정보");
        addBullet(content, "회원가입·프로필: 아이디, 비밀번호, 이름, 닉네임, 생년월일, 전화번호, 성별, 주소·상세주소, 프로필 사진");
        addBullet(content, "주행 기록: 주행 일시·시간·거리·속도·고도, 공개 여부, GPS 경로와 GPX 파일");
        addBullet(content, "사용자 작성 정보: 위험지역 제보의 위치, 주소, 내용과 첨부 사진; 코스 활동·스크랩·리뷰·미션 정보");
        addBullet(content, "기기 내 정보: 로그인 상태·프로필 표시 정보·SOS 연락처, 그리고 저장 전 주행 복구용 임시 경로 데이터");
        addBullet(content, "선택 기능: Ride Assistant가 사용 설정된 경우 안내 생성을 위해 주행 텔레메트리가 설정된 연동 서버로 전송될 수 있음");

        addSection(content, "로그아웃과 계정 삭제");
        addBody(content, "로그아웃은 이 기기에 남은 로그인·프로필·SOS 연락처와 임시 주행 데이터를 지우지만, 서버 계정과 저장된 주행 기록·GPX·사진은 유지합니다. "
                + "계정 삭제는 서버 데이터까지 삭제하거나 익명화하는 별도 절차입니다.");

        Button accountDeletion = actionButton(
                TextUtils.isEmpty(BuildConfig.TRAILGO_ACCOUNT_DELETION_URL)
                        ? "계정 삭제"
                        : "계정 삭제 웹페이지 열기",
                Color.rgb(198, 40, 40)
        );
        accountDeletion.setOnClickListener(view -> {
            if (TextUtils.isEmpty(BuildConfig.TRAILGO_ACCOUNT_DELETION_URL)) {
                showAccountDeletionStatus();
            } else {
                openUrl(BuildConfig.TRAILGO_ACCOUNT_DELETION_URL, "계정 삭제 페이지를 열 수 없습니다.");
            }
        });
        content.addView(accountDeletion, matchWidth());

        addSection(content, "개인정보처리방침");
        if (TextUtils.isEmpty(BuildConfig.TRAILGO_PRIVACY_POLICY_URL)) {
            addBody(content, "현재는 인앱 안내를 표시하고 있습니다. 출시 전 공개 HTTPS 정책 URL을 설정하면 이 자리에서 해당 정책 페이지를 열 수 있습니다.");
        } else {
            Button openPolicy = actionButton("공식 개인정보처리방침 열기", GREEN);
            openPolicy.setOnClickListener(view -> openUrl(
                    BuildConfig.TRAILGO_PRIVACY_POLICY_URL,
                    "정책 페이지를 열 수 없습니다."
            ));
            content.addView(openPolicy, matchWidth());
        }

        addSection(content, "내가 확인할 수 있는 사항");
        addBody(content, "기록 공개 범위는 설정 > 개인정보에서 정할 수 있습니다. 위치 권한은 주행 시작 시에만 요청되며, 기기 설정에서 언제든 변경할 수 있습니다. "
                + "위치 권한을 허용하지 않으면 주행 기록 기능은 시작할 수 없습니다.");
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showAccountDeletionStatus() {
        new AlertDialog.Builder(this)
                .setTitle("계정 삭제 준비 필요")
                .setMessage("현재 연결된 TrailGo 서버에는 본인 인증을 포함한 계정 삭제 API가 없습니다. "
                        + "지금은 계정·주행 기록·GPX·업로드 사진을 삭제할 수 없으며, 이 버튼을 눌러도 로그아웃이나 로컬 데이터 삭제가 실행되지 않습니다.\n\n"
                        + "서버에 인증된 탈퇴 API와 데이터 삭제 정책이 구현된 뒤에만 실제 계정 삭제를 제공할 수 있습니다.")
                .setPositiveButton("확인", null)
                .show();
    }

    private void openUrl(String url, String errorMessage) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        }
    }

    private void addTitle(LinearLayout parent, String value) {
        TextView view = text(value, 23, TEXT);
        view.setGravity(Gravity.START);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        parent.addView(view, matchWidth());
    }

    private void addSection(LinearLayout parent, String value) {
        TextView view = text(value, 16, TEXT);
        view.setTypeface(null, android.graphics.Typeface.BOLD);
        view.setPadding(0, dp(26), 0, dp(8));
        parent.addView(view, matchWidth());
    }

    private void addBody(LinearLayout parent, String value) {
        TextView view = text(value, 14, MUTED);
        view.setLineSpacing(dp(3), 1f);
        parent.addView(view, matchWidth());
    }

    private void addBullet(LinearLayout parent, String value) {
        TextView view = text("• " + value, 14, MUTED);
        view.setLineSpacing(dp(3), 1f);
        view.setPadding(0, dp(3), 0, 0);
        parent.addView(view, matchWidth());
    }

    private Button actionButton(String label, int color) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(label);
        button.setTextColor(Color.WHITE);
        button.setTextSize(14);
        button.setBackgroundColor(color);
        LinearLayout.LayoutParams params = matchWidth();
        params.topMargin = dp(8);
        button.setLayoutParams(params);
        return button;
    }

    private LinearLayout.LayoutParams matchWidth() {
        return new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView text(String value, int sizeSp, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        return view;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
