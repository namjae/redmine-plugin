package hudson.plugins.redmine;

import hudson.Extension;
import hudson.MarkupText;
import hudson.MarkupText.SubText;
import hudson.model.AbstractBuild;
import hudson.scm.ChangeLogAnnotator;
import hudson.scm.ChangeLogSet.Entry;

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * Annotates <a href="http://www.redmine.org/wiki/redmine/RedmineSettings#Referencing-issues-in-commit-messages">RedmineLink</a>
 * notation in changelog messages.
 * 
 * @author gaooh
 * @date 2008/10/13
 */
@Extension
public class RedmineLinkAnnotator extends ChangeLogAnnotator {

	private static boolean isVersionBefore120;
	
	@Override
	public void annotate(AbstractBuild<?, ?> build, Entry change, MarkupText text) {
		RedmineProjectProperty rpp = build.getProject().getProperty(RedmineProjectProperty.class);
        if(rpp == null || rpp.getRedmineWebsite() == null) { // not configured
            return; 
        }

        String url = rpp.getRedmineWebsite().baseUrl;
        isVersionBefore120 = VersionUtil.isVersionBefore120(rpp.getRedmineWebsite().versionNumber);
        LinkMarkup[] markups = MARKUPS;
        if(isVersionBefore120) { 
        	markups = MARKUPS_OLD;
        } 
        
        for (LinkMarkup markup : markups) {
    		markup.process(text, url);
    	}
	}

	static final class LinkMarkup {
        private final Pattern pattern;
        private final String href;
        
        LinkMarkup(String pattern, String href) {
        	pattern = NUM_PATTERN.matcher(pattern).replaceAll("([\\\\d|,| |&|#]+)"); // \\\\d becomes \\d when in the expanded text.
        	pattern = ANYWORD_PATTERN.matcher(pattern).replaceAll("((?:\\\\w|[._-])+)");
            this.pattern = Pattern.compile(pattern);
            this.href = href;
        }

        void process(MarkupText text, String url) {
        	
        	for(SubText st : text.findTokens(pattern)) {
        		String[] message = st.getText().split(" ", 2);
        		
        		if (message.length > 1) {
        			String[] nums = message[1].split(",|&| ");
        			String splitValue = ",";
        			if(message[1].indexOf("&") != -1) {
        				splitValue = "&";
        			} else if(message[1].indexOf("#") != -1) {
        				splitValue = "#";
        			} else if(message[1].indexOf(" ") != -1) {
        				splitValue = " ";
        			}
        			
        			if(nums.length > 1) {
                		int startpos = 0;
        				int endpos = message[0].length() + nums[0].length() + 1;
        				nums[0] = nums[0].replace("#", "");
        				st.addMarkup(startpos, endpos, getIssuesUrl(url, nums[0]), "</a>");
        				
        				startpos = endpos + splitValue.length();
        				endpos = startpos;
        			
        				for(int i = 1 ; i < nums.length ; i++) {
        					endpos += nums[i].length() ;
        					if(i != 1) {
        						endpos += splitValue.length();
        					}
        					if(endpos >= st.getText().length()) {
        						endpos = st.getText().length();
        					}
        					if(StringUtils.isNotBlank(nums[i])) {
        						nums[i] = nums[i].replace("#", "");
        						st.addMarkup(startpos, endpos, getIssuesUrl(url, nums[i]), "</a>");
        					}
        					startpos = endpos + splitValue.length();
        					
        				}
        			} else {
        				st.surroundWith("<a href='"+url+href+"'>","</a>");
        			}
        		} else {
        			st.surroundWith("<a href='"+url+href+"'>","</a>");
        		}
    		}
        }

        private String getIssuesUrl(String url, String num) {
        	if(isVersionBefore120) {
        		return "<a href='"+url+"issues/show/"+num.trim() +"'>";
        	}
        	return "<a href='"+url+"issues/"+num.trim() +"'>";
        }
        
        private static final Pattern NUM_PATTERN = Pattern.compile("NUM");
        private static final Pattern ANYWORD_PATTERN = Pattern.compile("ANYWORD");
        
    }

    static final LinkMarkup[] MARKUPS = new LinkMarkup[] {
    	new LinkMarkup(
            "(?:#|refs |references |IssueID |fixes |closes )#?NUM",
            "issues/$1"),
        new LinkMarkup(
            "((?:[A-Z][a-z]+){2,})|wiki:ANYWORD",
            "wiki/$1$2"),
    };
    static final LinkMarkup[] MARKUPS_OLD = new LinkMarkup[] {
    	new LinkMarkup(
            "(?:#|refs |references |IssueID |fixes |closes )#?NUM",
            "issues/show/$1"),
        new LinkMarkup(
            "((?:[A-Z][a-z]+){2,})|wiki:ANYWORD",
            "wiki/$1$2"),
    };
}
