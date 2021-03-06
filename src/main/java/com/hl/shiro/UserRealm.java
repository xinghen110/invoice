package com.hl.shiro;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.LockedAccountException;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.UnknownAccountException;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.util.ByteSource;
import com.hl.dao.UserDao;
import com.hl.domain.Permission;
import com.hl.domain.User;
import com.hl.service.UserService;

public class UserRealm extends AuthorizingRealm {

	@Resource(name = "userDao")
	private UserDao userDao;
	
	@Resource(name = "userService")
	private UserService userService;
	
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		//授权
		//从 principals获取主身份信息
		User user = (User) principals.getPrimaryPrincipal();
		//根据身份信息获取用户权限信息
		List<Permission>permissions = user.getPermissions();
		if(permissions == null) permissions = userService.getAllPermission(user.getUser_id());
		//放到下面的数组中
		List<String>permission_strs = new ArrayList<>();
		Iterator<Permission>iterator = permissions.iterator();
		while(iterator.hasNext()){
			Permission permission = iterator.next();
			permission_strs.add(permission.getPermission_name());
			//System.out.println("权限名称为"+permission.getPermission_name());
		}
		SimpleAuthorizationInfo simpleAuthorizationInfo = new SimpleAuthorizationInfo();
		simpleAuthorizationInfo.addStringPermissions(permission_strs);
		System.out.println("UserRealm授权结束");
		return simpleAuthorizationInfo;
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
		//认证
		// token是用户输入的用户名和密码 
		// 第一步从token中取出用户名
		String user_name = (String) token.getPrincipal();
		// 第二步：根据用户输入的user_name从数据库查询
		User user = userDao.getUserByName(user_name);
		System.out.println(user_name);
		if(user == null){
			//抛出账户不存在的异常
			throw new UnknownAccountException();
		}
		if(user.getLocked() == 1){
			//抛出账户锁定异常
			throw new LockedAccountException();
		}
		String password = user.getUser_password();
		String salt = user.getSalt();
		//权限集合
		//根据身份信息获取用户权限信息
		try {
			List<Permission>list = userService.getAllPermission(user.getUser_id());
			user.setPermissions(list);
			//设置用户组集合
			user.setGroups(userDao.getUserGroups(user.getUser_id()));
		} catch (Exception e) {
			e.printStackTrace();
		}	
		//将User设置simpleAuthenticationInfo
		SimpleAuthenticationInfo authenticationInfo = new SimpleAuthenticationInfo(user, password,ByteSource.Util.bytes(salt), this.getName());
		System.out.println("UserRealm认证结束");
		return authenticationInfo;
	}
	
	@Override
	public void setName(String name) {
		super.setName("userRealm");
	}

}
