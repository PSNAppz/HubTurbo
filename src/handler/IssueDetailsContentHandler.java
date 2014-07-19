package handler;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.stream.Collectors;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.WeakListChangeListener;
import model.TurboComment;
import model.TurboIssue;

import org.eclipse.egit.github.core.Comment;

import service.ServiceManager;
import service.updateservice.CommentUpdateService;

public class IssueDetailsContentHandler {
	private TurboIssue issue;
	
	private ObservableList<Comment> allGhContent = FXCollections.observableArrayList();
	private ObservableList<TurboComment> comments = FXCollections.observableArrayList();
	private ObservableList<TurboComment> log = FXCollections.observableArrayList();
	
	private CommentUpdateService commentsUpdater;
	private ListChangeListener<Comment> commentsChangeListener;
	
	public IssueDetailsContentHandler(TurboIssue issue){
		this.issue = issue;
		setupContent();
	}
	
	private boolean isNotSetup(){
		return commentsUpdater == null || commentsChangeListener == null;
	}
	
	private void setupContent(){
		getDetailsContent();
		commentsUpdater = ServiceManager.getInstance().getCommentUpdateService(issue.getId(), allGhContent);
		setupCommentsChangeListener();
	}
	
	public ObservableList<TurboComment> getComments(){
		return comments;
	}
	
	public ObservableList<TurboComment> getIssueHistory(){
		return log;
	}
	
	public void startContentUpdate(){
		if(isNotSetup()){
			setupContent();
		}
		//TODO:
		if(commentsUpdater != null){
//			commentsUpdater.startCommentsListUpdate();			
		}
	}
	
	public void stopContentUpdate(){
		if(commentsUpdater != null){
//			commentsUpdater.stopCommentsListUpdate();			
		}
	}
	
	public void createComment(String text){
		try {
			Comment comment = ServiceManager.getInstance().createComment(issue.getId(), text);
			allGhContent.add(comment);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void editComment(TurboComment comment){
		try {
			Comment ghComment = comment.toGhComment();
			ServiceManager.getInstance().editComment(ghComment);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void deleteComment(TurboComment comment){
		try {
			ServiceManager.getInstance().deleteComment(comment.getId());
			removeCachedItem(comment.getId());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void removeCachedItem(long commentId){
		for(Comment item: allGhContent){
			if(item.getId() == commentId){
				allGhContent.remove(item);
				return;
			}
		}
	}
	
	private void setupCommentsChangeListener(){
		WeakReference<IssueDetailsContentHandler> selfRef = new WeakReference<>(this);
		commentsChangeListener = new ListChangeListener<Comment>(){

			@Override
			public void onChanged(
					javafx.collections.ListChangeListener.Change<? extends Comment> arg0) {
				IssueDetailsContentHandler self = selfRef.get();
				if(self != null){
					self.updateData();
				}
			}
		};
		WeakListChangeListener<Comment> listener = new WeakListChangeListener<>(commentsChangeListener);
		allGhContent.addListener(listener);
	}
	
	private void updateData(){
		updateCommentsList();
		updateLogContents();
	}
	
	private void getDetailsContent(){
		try {
			allGhContent.clear();
			List<Comment> allItems = ServiceManager.getInstance().getComments(issue.getId());
			allGhContent.addAll(allItems);
			updateData();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updateCommentsList(){
		List<TurboComment> filteredComments = allGhContent.stream()
												   .map(item -> new TurboComment(item))
												   .filter(item -> !item.isIssueLog())
												   .collect(Collectors.toList());
		for(TurboComment item : filteredComments){
			updateItemInCommentsList(item);
		}
	}
	
	private void updateItemInCommentsList(TurboComment comment){
		for(TurboComment item : comments){
			if(item.getId() == comment.getId()){
				item.copyValues(comment);
				return;
			}
		}
		comments.add(comment);
	}
	
	private void updateLogContents(){
		List<TurboComment> logItems = allGhContent.stream()
										   .map(item -> new TurboComment(item))
				   						   .filter(item -> item.isIssueLog())
				   						   .collect(Collectors.toList());
		log.clear();
		log.addAll(logItems);
	}
	
	@Override
	public void finalize(){
		commentsUpdater.stopCommentsListUpdate();
	}
}
