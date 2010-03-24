package ac.uk.notts.mrl.MyJourneyDiary;
/**
 * @author jef@cs.nott.ac.uk 
 * The webview for the online questionnaire, put URL in here.
 */

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class OnlineQuestionnaire extends Activity {
	
	WebView mWebView;
	
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.questionnaire);

	    mWebView = (WebView) findViewById(R.id.webview);
	    mWebView.getSettings().setJavaScriptEnabled(true);
	    //URL needs to be replaced if we decide to use this at all
	    mWebView.loadUrl("http://www...");
	    
	    mWebView.setWebViewClient(new QuestionnaireWebViewClient());
	}
	
	private class QuestionnaireWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	        view.loadUrl(url);
	        return true;
	    }
	    
	    public boolean onKeyDown(int keyCode, KeyEvent event) {
	        if ((keyCode == KeyEvent.KEYCODE_BACK) && mWebView.canGoBack()) {
	            mWebView.goBack();
	            return true;
	        }
	        return false;
	    }
	}

}
