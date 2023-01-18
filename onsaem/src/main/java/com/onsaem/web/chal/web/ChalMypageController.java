package com.onsaem.web.chal.web;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.onsaem.web.chal.service.BankService;
import com.onsaem.web.chal.service.ChalService;
import com.onsaem.web.chal.service.ChalVO;
import com.onsaem.web.chal.service.MediaService;
import com.onsaem.web.chal.service.NgoService;
import com.onsaem.web.chal.service.ParticipantService;
import com.onsaem.web.chal.service.ParticipantVO;
import com.onsaem.web.chal.service.ProofService;
import com.onsaem.web.chal.service.ProofVO;
import com.onsaem.web.common.service.LikeVO;
import com.onsaem.web.common.service.MediaVO;
import com.onsaem.web.common.service.PaymentVO;
import com.onsaem.web.common.service.RefundVO;
import com.onsaem.web.common.service.RepliesVO;
import com.onsaem.web.member.service.MemberService;

@Controller
@CrossOrigin(origins="*")
@RequestMapping("/mypage")
public class ChalMypageController {

	@Autowired ChalService chalService;
	@Autowired ProofService proofService;
	@Autowired ParticipantService partService;
	@Autowired MemberService memService;
	@Autowired BankService bankService;
	@Autowired NgoService ngoService;

	
	//권한 - 일반회원의 챌린저스 마이페이지 첫화면, 진행중 챌린지 띄우깅
	@RequestMapping(value="/myCurrentChal",method=RequestMethod.GET)
	public String myCurrentChalList(Model model,ChalVO vo, HttpServletRequest req,ProofVO pvo ) {
		HttpSession session = req.getSession();
//		//세션에서 가져온 로그인 된 id값
		String id = (String)session.getAttribute("id");
		
		vo.setParticipantId("hodu");
		//로그인 유저가 참가중인 챌린저들,,,
		model.addAttribute("chals", chalService.myCurentChal(vo));
		
		List<ChalVO> list=chalService.myCurentChal(vo);
		List<Integer> plist = new ArrayList<>();
		
		for(int i=0;i<list.size();i++) {
			
			pvo.setChalId(list.get(i).getChalId());
			pvo.setProofWriter("hodu");
			Integer n = proofService.checkProof(pvo);
			plist.add(n);
			
		}
		System.out.println(plist);
		
		//썸네일덜 ~ 나중에 디자인 하기 싫으면 걍 뺴고 표로 만들면 됨 ~
		model.addAttribute("pics", proofService.myChalThumnails(vo));
		
		//인증했는지 안했는지 확인용 
		model.addAttribute("checks", plist);
		
		return "content/challengers/MyCurrentChal";
	}
	
	//인증샷 넣기 ! 
	@RequestMapping(value="/myCurrentChal",method=RequestMethod.POST)
	public String inputProof(@RequestBody ProofVO pvo, HttpServletRequest req) {
		HttpSession session = req.getSession();
		MediaVO vo = new MediaVO();

		
//		//세션에서 가져온 로그인 된 id값
		String id = (String)session.getAttribute("id");
		
		//인증테이블에 값넣기
		pvo.setProofWriter("hodu");
		pvo.setCondition("정상");
		proofService.inputProof(pvo);
		
		//미디어 테이블에 값넣기
		String realFolder = "/challengers/img/";
		
		vo.setGroupId(pvo.getProofId());
		vo.setFileName(pvo.getFileName());
		vo.setFileRoute(realFolder);
		vo.setSubGroup("챌린저스인증");
		proofService.inputMedia(vo);
		return "redirect:/mypage/myCurrentChal";
	}
	
	//권한 - 일반회원의 챌린저스 마이페이지 - 시작전 챌린지 모음
	@RequestMapping(value="/myBeforeChal",method=RequestMethod.GET)
	public String myBeforeChalList(Model model,ChalVO vo, HttpServletRequest req, Authentication authentication) {
		
		//세션에서 가져온 로그인 된 id값 
		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		
		vo.setParticipantId(userDetails.getUsername());
		//이용자가 참가한 시작 전인 챌린지들 가져오기.
		List<ChalVO> list=chalService.myBeforeChal(vo);
		System.out.println(list);
		System.out.println(list.size());
		
		if(list.size()>0){
			model.addAttribute("chals", list);
		} else {
			model.addAttribute("msg", "시작 예정인 챌린지가 없습니다!");
		}
		
		//썸네일 조회
		model.addAttribute("pics", proofService.myChalThumnails(vo));
		
		//은행들 가져오기
		model.addAttribute("banks", bankService.listBank());

		//개인 정보 가져오기
		model.addAttribute("user", memService.getMember("hodu"));
		
		return "content/challengers/MyBeforeChal";
	}
	
	//마이페이지 시작전 챌린지 모음 - 취소하기
	@RequestMapping(value="/myBeforeChal",method=RequestMethod.POST)
	@ResponseBody
	public String CancelChal(@RequestBody RefundVO rvo,ParticipantVO pvo, PaymentVO yvo, Authentication authentication) {
		UserDetails userDetails = (UserDetails) authentication.getPrincipal();

		//환불테이블에 추가
		
		partService.inputRefund(rvo);
		
		//participant테이블에서 삭제갈김 - 이것도 update로 해야할지 판단필요
		pvo.setChalId(rvo.getGroupId());
		pvo.setParticipantId(userDetails.getUsername());
		partService.delParticipant(pvo);
		
		//결제 테이블에서 update
		yvo.setPaymentId(rvo.getPaymentId());
		partService.updateForRefund(yvo);
		
		
		
		return "content/challengers/MyBeforeChal";
	}
		
		
	//권한 - 일반회원의 챌린저스 마이페이지- 완료된 챌린지 모음
	@RequestMapping(value="/myEndChal",method=RequestMethod.GET)
	public String myEndChalList(Model model,ChalVO vo, Authentication authentication) {
		UserDetails userDetails = (UserDetails) authentication.getPrincipal();
		vo.setParticipantId(userDetails.getUsername());
		model.addAttribute("chals", chalService.myEndChal(vo));
		
		return "content/challengers/MyEndChal2";
	}
	
	//마이페이지의 나의 인증현황
	@RequestMapping(value="/myChalStatus", method=RequestMethod.GET)
	public String myChalStatus(Model model, @RequestParam(value="chalId", required=false)String chalId, MediaVO vo, ProofVO pvo,HttpServletRequest request) {
//		HttpSession session=request.getSession();
//		//세션에서 가져온 로그인 된 id값
//		String id = (String)session.getAttribute("id");
		chalId="CH1";
//		//챌린저스 한건 정보
		model.addAttribute("chal",chalService.getChal(chalId));
		
		ChalVO cvo = chalService.getChal(chalId);
		Date end = cvo.getEndDate();
		Date start = cvo.getStartDate();
		Date today = new Date();
		long dday = (today.getTime() - end.getTime())/(24*60*60*1000)+1;
		model.addAttribute("dday", dday);
		long days = (end.getTime() - start.getTime())/(24*60*60*1000)+1;

		model.addAttribute("total", days);
		
		//		
//		//해당 챌린지에 대해 내가 입력한 모든 인증샷 싹다 가져오기 - chalId, memberid
//		vo.setGroupId(chalId);
//		vo.setProofWriter(id);
//		model.addAttribute("myPhotoes", proofService.getMyShotsForOne(vo));
//		
//		//Proofs 테이블에서 count(*) 해야함 - 조건이 성공인거, 작성자 아이디, 챌린지 아이디 필요
		pvo.setChalId(chalId);
		pvo.setProofWriter("hodu");
		pvo.setCondition("정상");
		
		model.addAttribute("cntGood", proofService.countProof(pvo));
		
//		//Proofs 테이블에서 count(*) 해야함 - 조건이 성공인거, 작성자 아이디, 챌린지 아이디 필요
		pvo.setChalId(chalId);
		pvo.setProofWriter("hodu");
		pvo.setCondition("실패");
		
		model.addAttribute("cntBad", proofService.countProof(pvo));
		
		return "content/challengers/MyChalStatus1";
	}
	
	
	//얘 마이페이지의 - 2번째 페이지, value 파일명 다고치삼
	@RequestMapping(value="/myChalStatus2", method=RequestMethod.GET)
	public String myChalStatus2(Model model, @RequestParam(value="chalId", required=false)String chalId) {
		//한 챌린지에 대한 모든 사람들의 사진을 가져오기
		chalId = "CH1";
		//한 챌린지에 대한 모든 사람들의 인증글 가져오기
		model.addAttribute("proofs", proofService.listProofAll(chalId));
		return "content/challengers/MyChalStatus2";
	}
	
	//마이페이지 2번쨰 페이지의 모달창내용 ㅎㅎ
	@RequestMapping(value="/proofDetail", method=RequestMethod.POST)
	@ResponseBody
	public Map<String, Object> proofDetail(Model model, @RequestBody String proofId){		
		 Map<String, Object> map = new HashMap<String, Object>();
		  //게시글 한개 내용 가져오기, 인증샷 model.addAttribute("proof",
		 map.put("brd",proofService.getProof(proofId));
		 
		 //댓글 리스트 
		 map.put("repllies",proofService.listReply(proofId));
		//좋아효용 가져오기
		  LikeVO vo = new LikeVO(); 
		  vo.setGroupId(proofId); 
		 map.put("likeCnt", proofService.cntChalLike(vo));
		
		 return map ;
		 
	}
	
	@RequestMapping(value="/addChalLike", method=RequestMethod.POST)
	@ResponseBody
	public Integer addChalLike(LikeVO vo, Authentication authentication) {
		//좋아요 넣기
		UserDetails userDetails = (UserDetails)authentication.getPrincipal();
		String id = userDetails.getUsername();
		vo.setMemberId(id);
		
		proofService.inputLike(vo);
		//좋아요 갯수
		proofService.cntChalLike(vo);
		return proofService.cntChalLike(vo);
	}
	
	@RequestMapping(value="/delLike", method=RequestMethod.POST)
	@ResponseBody
	public Integer delChalLike(LikeVO vo, Authentication authentication) {
		
		UserDetails userDetails = (UserDetails)authentication.getPrincipal();
		String id = userDetails.getUsername();
		vo.setMemberId(id);
		//좋아요 삭제
		proofService.delLike(vo);
		//좋아요 조회해서 가져와야할듯,,
		proofService.cntChalLike(vo);
		return proofService.cntChalLike(vo);
	}
	
	//수정필요 댓글 작성 ㅎㅎ
	@RequestMapping(value="/inputReply", method=RequestMethod.POST)
	@ResponseBody
	public String inputReply(@RequestBody RepliesVO vo) {
		proofService.inputReply(vo);
		
		//댓글 리스트 가져오기? //댓글한건? 멀 가져와야될까?
		return "content/challengers/MyChalStatus2";
	}
	
	//내가 신청한 ngo목록 보기,,안하면 안되나~? ㅎㅎ
	@RequestMapping(value="/myApplyNgo", method=RequestMethod.GET)
	public String myApplyNgo(Model model) {
		//로그인 아이디 불러오기
		
		//내가 신청한 ngo리스트를 가져오기
		String id = "hodu"; //나중에 세션으로 갈아끼우기
		
		if(ngoService.myApplies(id).size()==0) {
			model.addAttribute("msg", "신청하신 목록이 없습니다.");
		}else {
			model.addAttribute("ngoes", ngoService.myApplies(id));
		}
		
		return "content/challengers/MyApplyNgo";
	}
	

}
