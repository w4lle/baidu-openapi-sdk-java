1��BaiduOAuth2Client��װ�˻��OAuth2��Ȩ����ط�����BaiduApiClient��װ�˽���api�������ط���
2��Baidu���з�װ�˶�token��Ϣ�洢���߼�������ʹ���п��Եֿ�csrf������
	a)����BaiduStore store ;ͨ��ʹ��Cookieʵ�֡�
			BaiduStore store = new BaiduCookieStore(clientId,request,response);
	b)����Baidu����
			Baidu baidu = new Baidu(clientId,clientSecret,redirectUri,store,request);
	c)��ȡAccessToken����Ϣ
			String accessToken = baidu.getAccessToken();