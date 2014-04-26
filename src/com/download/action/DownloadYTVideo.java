package com.download.action;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This will download Youtube file from the given URL.
 * Usage : DownloadYTVideo Youtube_URL1 [ Youtube_URL2]
 * 
 * If you tube decides to change the format of the html, this will stop working.
 * 
 * The extension is hardcoded to ".mp4"
 * VLC Player can be used to run the video.
 * 
 * @version 1.0
 *
 */
public class DownloadYTVideo {

	public static void main(String[] args) 
	{
        try {

        		if(args.length == 1)
        		{
        			System.out.println("Usage: DownloadYTVideo URL1 [URL2]");
        		}
        		
        		DownloadYTVideo downloader = new DownloadYTVideo();
        		ArrayList<String> arrURLList = downloader.getListOfURLs(args);
        		
        		downloader.invokeDownloader(arrURLList,"/home/kjohn");
                
            }
            catch ( Exception e ) {

                System.err.println( e.getMessage() );
            }
        
    }
	
	/**
	 * Get list of URLs from the invocation parameters.
	 * @param args
	 * @return
	 */
	private ArrayList<String> getListOfURLs(String[] args)
	{
		ArrayList<String> arrURLList = new ArrayList<String>();
		
		for(String sUrl : args)
		{
			arrURLList.add(sUrl);
		}
		return arrURLList;
	}
	
	public String invokeDownloader(ArrayList<String> arrUrls, String sDownloadLocation)
	{
		String sMessage = "";
		Integer urlNum = 1;
		for(String sUrls : arrUrls)
		{
			System.out.println("Downloading " + sUrls + " (" + urlNum + " of " + arrUrls.size()+")");
			
			Youtube youtube = getYoutubeObj(sUrls);
			
			sMessage = downloadVideo(youtube,sDownloadLocation);
			
			urlNum++;
		}
		return sMessage;
	}
	
	public String invokeDownloader(String sUrl, String sDownloadLocation)
	{
		String sMessage = "";
		Youtube youtube = getYoutubeObj(sUrl);
		sMessage = downloadVideo(youtube,sDownloadLocation);
		return sMessage;
	}
	
	/**
	 * This method will do the actual download of the stream
	 * The location to where it has to be downloaded has been hard coded.
	 * This can be changed or passed as a parameter.
	 * @param youtube
	 */
	private String downloadVideo(Youtube youtube, String sDownloadLocation)
	{
		String sErrorMessg = "";
        try {
        	System.out.println(youtube.getDownLoadURL());
        	URL urlVideoURL = new URL(youtube.getDownLoadURL());
            URLConnection urlConnection = urlVideoURL.openConnection();
            InputStream inStreamVideo = urlConnection.getInputStream();
            
            OutputStream outStreamVideo = new BufferedOutputStream(new FileOutputStream(sDownloadLocation+"/"+youtube.getFileName()+".mp4"));
            for (int b; (b = inStreamVideo.read()) != -1; ) {
            	outStreamVideo.write(b);
            }
            outStreamVideo.close();
            inStreamVideo.close();
        	
        } catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
        catch (FileNotFoundException e )
        {
        	sErrorMessg = "NoPerm";
            e.printStackTrace();
        }
        catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			sErrorMessg = "IOException";
		}
        return sErrorMessg;
	}
	
	/**
	 * This will open a normal Youtube video, and parse through the html to retrieve the the stream URL and title name.
	 * @param sUserYoutubeURl
	 * @return
	 */
	private Youtube getYoutubeObj(String sUserYoutubeURl)
	{
		Youtube youtube = new Youtube(sUserYoutubeURl);
		try {
			URL youtubeVideo = new URL(sUserYoutubeURl);
			BufferedReader in = new BufferedReader(new InputStreamReader(	youtubeVideo.openStream()));
			
			String inputLine = "";
			boolean isDownUrlFound = false;
			boolean isTitleFound = false;
			while ((inputLine = in.readLine()) != null)
			{
				if (inputLine.contains("ytplayer.config"))
				{
					Pattern pattern = Pattern.compile("\"url_encoded_fmt_stream_map\": \".*?\"");
					Matcher m = pattern.matcher(inputLine);
					String urlStrings = null;
					String sVideoURL = null;
					if (m.find()) {
						urlStrings = m.group();
						urlStrings = urlStrings.substring(31, m.group().length() - 1);
						
						for (String var : urlStrings.split(",")) {
							if (var.contains("type=video%2Fmp4")) {
								sVideoURL = rebuild(URLDecoder.decode(var).replace("\\u0026", "&"));
								break;
							}
						}

						youtube.setDownLoadURL(sVideoURL);
						isDownUrlFound = true;
					}
				}
				else if(inputLine.contains("<meta name=\"title\"") && !isTitleFound)
				{
					//retrieve the title name.
					
					inputLine = inputLine.trim();
                    Integer iStartIndex =  (inputLine.indexOf("<meta name=\"title\" content=\"")+28);
                    String sStartString = inputLine.substring( iStartIndex );
                    Integer iEndIndex =  sStartString.indexOf("\">");
					String sVideoTitle = sStartString.substring( 0 , iEndIndex );
					
					sVideoTitle = sVideoTitle.replace(" ", "_");
					youtube.setFileName(sVideoTitle);
					isTitleFound = true;
				}
				
				if(isDownUrlFound && isTitleFound)
				{	//need not parse the whole file if the required information is found.
					break;
				}
			}
			
			in.close();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return youtube;
	}
	
	private String rebuild(String param) {
		String ex[] = {"type", "fallback_host", "quality"};
		List excludeList = Arrays.asList(ex);

		String url = null;
		String query = "";
		for (String var: param.split("&")) {
			if (var.split("=")[0].equals("url")) {
				url = var.replace("url=", "");
			} else if (!excludeList.contains(var.split("=")[0])) {
				query = query + "&" + var;
			}
		}
		return url + query.replaceFirst("itag=[0-9][0-9]&", "");
	}
}
