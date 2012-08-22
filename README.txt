1、BaiduOAuth2Client封装了获得OAuth2授权的相关方法，BaiduApiClient封装了进行api请求的相关方法
2、Baidu类中封装了对token信息存储的逻辑，并在使用中可以抵抗csrf攻击。
	a)创建BaiduStore store ;通常使用Cookie实现。
			BaiduStore store = new BaiduCookieStore(clientId,request,response);
	b)创建Baidu对象
			Baidu baidu = new Baidu(clientId,clientSecret,redirectUri,store,request);
	c)获取AccessToken等信息
			String accessToken = baidu.getAccessToken();