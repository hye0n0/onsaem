package com.onsaem.web.member.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface MemberService {

	// 회원단건조회
	public MemberVO getMember(String memberId);

	// 아이디 체크
	public MemberVO checkId(String memberId);

	// 이메일로 회원조회
	public String searchEmail(String email);

	// 이메일 인증
	public String sendAuthMail(String email);

	// 회원등록
	public String insertMember(MultipartFile[] profileFile, MemberVO member) throws IllegalStateException, IOException;

	// 아이디 보내기
	public String sendIdMail(String email);

	// 회원정보 변경
	public int updateMember(MemberVO member);

}
