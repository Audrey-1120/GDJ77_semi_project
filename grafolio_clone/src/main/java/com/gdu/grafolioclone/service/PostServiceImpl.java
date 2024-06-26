package com.gdu.grafolioclone.service;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.gdu.grafolioclone.dto.CommentDto;
import com.gdu.grafolioclone.dto.LikeDto;
import com.gdu.grafolioclone.dto.PostDto;
import com.gdu.grafolioclone.dto.UserDto;
import com.gdu.grafolioclone.mapper.PostMapper;
import com.gdu.grafolioclone.utils.MyFileUtils;
import com.gdu.grafolioclone.utils.MyPageUtils;
import com.gdu.grafolioclone.utils.MySecurityUtils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class PostServiceImpl implements PostService {

  private final PostMapper postMapper;
  private final MyPageUtils myPageUtils;
  private final MyFileUtils myFileUtils;
  
  @Override
  public ResponseEntity<Map<String, Object>> summernoteImageUpload(MultipartFile multipartFile) {
    
    // 이미지 저장할 경로 생성
    String uploadPath = myFileUtils.getUploadPath();
    File dir = new File(uploadPath);
    if(!dir.exists()) {
      dir.mkdirs();
    }
    
    // 이미지 저장할 이름 생성
    String filesystemName = myFileUtils.getFilesystemName(multipartFile.getOriginalFilename());
    
    
    // 실제 저장
    File file = new File(dir, filesystemName);
    try {
      multipartFile.transferTo(file);
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    
    // 이미지가 저장된 경로를 Map 으로 반환
    // view 단으로 보낼 src = "/grafolioclone/upload/2024/03/27/1234567890.jpg"
    // servlet-context.xml 에서 <resources> 태그를 추가한다. 
    // <resources mapping="/upload/**" location="file:///upload/" />
    Map<String, Object> map = Map.of("src", uploadPath + "/" + filesystemName);
    
    return new ResponseEntity<>(map, HttpStatus.OK);
  }

  @Override
  public int registerPost(HttpServletRequest request) {
    
    // 요청 파라미터
    String title = request.getParameter("title");
    String contents = request.getParameter("contents");
    int category = Integer.parseInt(request.getParameter("category"));
    int userNo = Integer.parseInt(request.getParameter("userNo"));

    // UserDto + PostDto 객체 생성
    UserDto user = new UserDto();
    user.setUserNo(userNo);
    PostDto post = PostDto.builder()
                     .title(MySecurityUtils.getPreventXss(title))
                     .contents(MySecurityUtils.getPreventXss(contents))
                     .category(category)
                     .user(user)
                   .build();

    /* summernote 편집기에서 사용한 이미지 확인하는 방법 (Jsoup 라이브러리) */
    Document document = Jsoup.parse(contents);
    Elements elements = document.getElementsByTag("img");
    if(elements != null) {
      for(Element element : elements) {
        String src = element.attr("src");
        /* src 정보를 DB에 저장하는 코드 등이 이 곳에 있으면 된다. */
        System.out.println(src);
      }
    }

    // DB에 post 저장
    return postMapper.insertPost(post);
	}
	
	@Override
	public ResponseEntity<Map<String, Object>> getPostList(HttpServletRequest request) {
		
		// 전체 Post 게시글 수
		int total = postMapper.getPostCount();
		
		// 스크롤 이벤트마다 가져갈 목록 개수
		int display = Integer.parseInt(request.getParameter("display") == null ? "12" : request.getParameter("display"));
		
		// 현재 페이지 번호
		int page = Integer.parseInt(request.getParameter("page"));
		
		// 카테고리 번호
		int category = Integer.parseInt(request.getParameter("category") == null ? "0" : request.getParameter("category"));
		
		// 페이징 처리에 필요한 정보 처리
		myPageUtils.setPaging(total, display, page);
		
		// DB 로 보낼 Map 생성
		Map<String, Object> map = Map.of("begin", myPageUtils.getBegin()
																	  ,"end", myPageUtils.getEnd()
																	  ,"category", category);
		List<PostDto> postList = null;
		
		// DB 에서 목록 가져오기
		if(category == 0) {
			postList = postMapper.getPostList(map);
		} else {
			postList = postMapper.getPostListByCategory(map);
		}
			
		
		return new ResponseEntity<Map<String,Object>>(Map.of("postList", postList,
																												 "totalPage", myPageUtils.getTotalPage())
																						  	, HttpStatus.OK);
	}
	
	@Override
	public ResponseEntity<Map<String, Object>> searchPosts(HttpServletRequest request) {
		
    // 요청 파라미터
    String query = request.getParameter("q");
    
    // 검색 데이터 개수를 구할 때 사용할 Map 생성
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("q", query);
    
    // 검색 데이터 개수 구하기
    int total = postMapper.getSearchCount(map);
    
    // 한 페이지에 표시할 검색 데이터 개수
    int display = Integer.parseInt(request.getParameter("display") == null ? "12" : request.getParameter("display"));
    
    // 현재 페이지 번호
    Optional<String> opt = Optional.ofNullable(request.getParameter("page"));
    int page = Integer.parseInt(opt.orElse("1"));
    
    // 페이징 처리에 필요한 처리
    myPageUtils.setPaging(total, display, page);
    
    // 검색 목록을 가져오기 위해서 기존 Map 에 begin 과 end 를 추가
    map.put("begin", myPageUtils.getBegin());
    map.put("end", myPageUtils.getEnd());
    
    // 검색 목록 가져오기
    List<PostDto> postList = postMapper.getSearchList(map);
    
    // 뷰로 전달할 데이터
		return new ResponseEntity<Map<String,Object>>(Map.of("postList", postList,
                                                				 "totalPage", myPageUtils.getTotalPage())
                                            					 , HttpStatus.OK);
	}
	
  @Override
  public int updateHit(int postNo) {
    return postMapper.updateHit(postNo);
  }

  @Override
  public PostDto getPostByNo(int postNo) {
    return postMapper.getPostByNo(postNo);
  }
  
  @Override
  public int modifyPost(PostDto post) {
  	return postMapper.updatePost(post);
  }
  
  @Override
  public int deletePost(int postNo) {
  	
  	// 포스트 번호로 코멘트 삭제
  	postMapper.removeCommentByPostNo(postNo);
  	
  	// 포스트 삭제
  	postMapper.removePost(postNo);

  	// 추후 수정 필요
  	return 0;
  }
  
  @Override
  public int registerComment(HttpServletRequest request) {
    
    // 요청 파라미터
    String contents = MySecurityUtils.getPreventXss(request.getParameter("contents"));
    int postNo = Integer.parseInt(request.getParameter("postNo"));
    int userNo = Integer.parseInt(request.getParameter("userNo"));
    
    // UserDto 객체 생성
    UserDto user = new UserDto();
    user.setUserNo(userNo);
    
    // CommentDto 객체 생성
    CommentDto comment = CommentDto.builder()
                                .contents(contents)
                                .user(user)
                                .postNo(postNo)
                              .build();
    
    return postMapper.insertComment(comment);
  }
  
  @Override
  public Map<String, Object> getCommentList(HttpServletRequest request) {
    
    // 요청 파라미터
    int postNo = Integer.parseInt(request.getParameter("postNo"));
    int page = Integer.parseInt(request.getParameter("page"));
    
    // 전체 댓글수
    int total = postMapper.getCommentCount(postNo);
    
    // 한 페이지에 표시할 댓글 개수
    int display = Integer.parseInt(request.getParameter("display") == null ? "10" : request.getParameter("display"));
    
    // 페이징 처리
    myPageUtils.setPaging(total, display, page);
    
    // 목록을 가져올 때 사용할 Map 생성
    Map<String, Object> map = Map.of("postNo", postNo
                                     ,"begin", myPageUtils.getBegin()
                                     , "end", myPageUtils.getEnd());
    
    // 결과 (목록, 페이징) 반환
    
    
    return Map.of("commentList", postMapper.getCommentList(map)
                , "paging", myPageUtils.getAsyncPaging());
  }
  
  @Override
  public int registerReply(HttpServletRequest request) {
    // 요청 파라미터
    String contents = MySecurityUtils.getPreventXss(request.getParameter("contents"));
    int postNo = Integer.parseInt(request.getParameter("postNo"));
    int userNo = Integer.parseInt(request.getParameter("userNo"));
    int groupNo = Integer.parseInt(request.getParameter("groupNo"));
  
    // UserDto 객체 생성
    UserDto user = new UserDto();
    user.setUserNo(userNo);
    
    // CommentDto 객체 생성
    CommentDto comment = CommentDto.builder()
                                .contents(contents)
                                .user(user)
                                .postNo(postNo)
                                .groupNo(groupNo)
                              .build();
    
    return postMapper.insertReply(comment);
  }
  
  @Override
  public int removeComment(int commentNo) {
    return postMapper.removeComment(commentNo);
  }
  
  @Override
  public int registerLike(Map<String, Object> params) {
    int postNo = (int) params.get("postNo");
    int userNo = (int) params.get("userNo");
    
    LikeDto like = LikeDto.builder()
    										.postNo(postNo)
    										.userNo(userNo)
    									.build();
    									
  	return postMapper.insertLike(like);
  }
  
  @Override
  public int removeLike(Map<String, Object> params) {
    int postNo = (int) params.get("postNo");
    int userNo = (int) params.get("userNo");
    
    LikeDto like = LikeDto.builder()
    										.postNo(postNo)
    										.userNo(userNo)
    									.build();
    									
  	return postMapper.removeLike(like);
  }
  
  @Override
  public int getLikeCount(int postNo) {
  	return postMapper.getLikeCountByPostNo(postNo);
  }
  
  @Override
  public int getLikeCountByUserNo(int userNo) {
  	return postMapper.getLikeCountByUserNo(userNo);
  }
  
@Override
  public int getHitCountByUserNo(int userNo) {
  	return postMapper.getHitCountByUserNo(userNo);
  }
  
  @Override
  public int checkLikeStatus(int postNo, int userNo) {
  	
    LikeDto like = LikeDto.builder()
    										.postNo(postNo)
    										.userNo(userNo)
    									.build();
    									
  	return postMapper.checkLikeStatus(like);
  }
  
  // 유저프로필 - 업로드한 게시글 가져오기(오채원)
  @Override
  public ResponseEntity<Map<String, Object>> getUserUploadList(HttpServletRequest request) {
    
    int total = postMapper.getUserUploadCount(Integer.parseInt(request.getParameter("userNo")));
    
    int display = Integer.parseInt(request.getParameter("display") == null ? "30" : request.getParameter("display"));
    
    int page = Integer.parseInt(request.getParameter("page"));
    
    myPageUtils.setPaging(total, display, page);
    
    int totalPage = myPageUtils.getTotalPage();
    
    Map<String, Object> map = Map.of("begin", myPageUtils.getBegin()
                                   , "end", myPageUtils.getEnd()
                                   , "userNo", Integer.parseInt(request.getParameter("userNo")));
    
    List<PostDto> userUploadList = postMapper.getUserUploadList(map);
    System.out.println(userUploadList);
    
    return ResponseEntity.ok(Map.of("userUploadList", userUploadList
                                  , "totalPage", totalPage));
  }
  
  
  // 유저프로필 - 좋아요한 게시글 가져오기(오채원)
  @Override
  public ResponseEntity<Map<String, Object>> getUserLikeList(HttpServletRequest request) {
    
    int total = postMapper.getUserLikeCountByUserNo(Integer.parseInt(request.getParameter("userNo")));
    
    int display = Integer.parseInt(request.getParameter("display") == null ? "30" : request.getParameter("display"));
    
    int page = Integer.parseInt(request.getParameter("page"));
    
    myPageUtils.setPaging(total, display, page);
    
    int totalPage = myPageUtils.getTotalPage();
    
    Map<String, Object> map = Map.of("begin", myPageUtils.getBegin()
                                   , "end", myPageUtils.getEnd()
                                   , "userNo", Integer.parseInt(request.getParameter("userNo")));
    List<PostDto> userLikeList = postMapper.getUserLikeListByUserNo(map);
    System.out.println(userLikeList);
    
    return ResponseEntity.ok(Map.of("userLikeList", userLikeList
                                  , "totalPage", totalPage));
  }
  
  // 게시글 상세 댓글수 (김규식)
  @Override
  public int getPostCommentCount(int postNo) {
    return postMapper.getCommentCountByPostNo(postNo);
  }
  
  //게시글 상세 조회수 (김규식)
  @Override
  public int getHitCountByPostNo(int postNo) {
    return postMapper.getHitCountByPostNo(postNo);
  }

  
}