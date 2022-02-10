package com.mt.ihttp.api

import com.google.gson.JsonElement
import com.mt.ihttp.base.BaseResponse
//import com.mt.ihttp.bean.*
//import com.mt.ihttp.bean.common.ActivityBean
//import com.mt.ihttp.bean.common.BannerListBean
//import com.mt.ihttp.bean.family.*
//import com.mt.ihttp.bean.libproject.PayDialogBean
//import com.mt.ihttp.bean.live.voiceroom.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 *
 *　　┏┓　　　┏┓+ +
 *　┏┛┻━━━┛┻┓ + +
 *　┃　　　　　　　┃ 　
 *　┃　　　━　　　┃ ++ + + +
 * ████━████ ┃+
 *　┃　　　　　　　┃ +
 *　┃　　　┻　　　┃
 *　┃　　　　　　　┃ + +
 *　┗━┓　　　┏━┛
 *　　　┃　　　┃　　　　　　　　　　　
 *　　　┃　　　┃ + + + +
 *　　　┃　　　┃
 *　　　┃　　　┃ +  神兽保佑
 *　　　┃　　　┃    代码无bug　　
 *　　　┃　　　┃　　+　　　　　　　　　
 *　　　┃　 　　┗━━━┓ + +
 *　　　┃ 　　　　　　　┣┓
 *　　　┃ 　　　　　　　┏┛
 *　　　┗┓┓┏━┳┓┏┛ + + + +
 *　　　　┃┫┫　┃┫┫
 *　　　　┗┻┛　┗┻┛+ + + +
 *
 * @Description: 所有apiService
 * @Author: CTS
 * @Date: 2020/9/22 15:52
 * @Note: 目录根据功能和业务分块
 *        目录: 1.通用api
 *             2.登录注册相关
 *             3.个人资料相关
 *             4.小游戏相关
 *             5.lib_project里的，临时放在这
 */
interface IHttpApiService {
    /**
     * ━━━━━━━━━━━━━━━━━━━
     * 通用api  （临时提示：一般包含通用上传，提交等全域通用接口）
     * ━━━━━━━━━━━━━━━━━━━
     */
//    //获取buvId
//    @FormUrlEncoded
//    @POST("/trace/getbuvid")
//    suspend fun getBuvId(@FieldMap maps: Map<String, String>): BaseResponse<BuvIdBean>
//
//    /**
//     * ━━━━━━━━━━━━━━━━━━━
//     * 同意协议之后激活
//     * ━━━━━━━━━━━━━━━━━━━
//     */
//    //获取buvId
//    @FormUrlEncoded
//    @POST("/trace/activate")
//    suspend fun traceActivate(@FieldMap maps: Map<String, String>): BaseResponse<Any>

    //文件下载
    @Streaming
    @GET
    suspend fun downloadFile(@Url fileUrl: String): Response<ResponseBody>

    /**
     * 文件上传
     * 暂时是用于图片上传。 因为旧逻辑是直接返回一个string，暂时先不改，后期建议改为bean
     * 注：包括多文件上传也用这个单文件上传接口，在上传工具类里进行循环上传即可（因为没有很复杂的多个大文件上传这种复杂需求，所以用这种方案。
     *     同时也可以避免一个文件上传失败导致所有文件都要重新上传，而且回调、进度等都更好处理）
     */
    @Multipart
    @POST()
    suspend fun uploadFile(
            @Url url: String,
            @Part parts: List<MultipartBody.Part>,
    ): BaseResponse<String?>

    /**
     * 适配java的方案
     * (java直接返回result，接收到result之后，再进行二次解析，多了一次Gson解析，会导致5ms左右的额外耗时)
     * 2021.11.30.修复bug：retrofit用Gson将response数据转换成any时，Gson在把String转换成int时默认转成double（这是Gson的一个bug）
     *      ，修改方案：将Any换成JsonElement接收
     */
    @FormUrlEncoded
    @POST()
    suspend fun post(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<JsonElement>

    /**
     * 适配java的方案
     * (java直接返回result，接收到result之后，再进行二次解析，多了一次Gson解析，会导致5ms左右的额外耗时)
     */
    @FormUrlEncoded
    @POST()
    suspend fun <T> postTest(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<T>


//    /**
//     * ━━━━━━━━━━━━━━━━━━━
//     * 登录注册相关
//     * ━━━━━━━━━━━━━━━━━━━
//     */
//    //临时测试模板
//    @FormUrlEncoded
//    @POST("/invite/regByInvitePhone")
//    suspend fun testPost2(@FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//
//    /**
//     * ━━━━━━━━━━━━━━━━━━━
//     * 个人资料相关
//     * ━━━━━━━━━━━━━━━━━━━
//     */
//    //获取个人资料
//    @FormUrlEncoded
//    @POST()
//    suspend fun getUserInfo(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<UserTipsData>
//
//
//    /**
//     * ━━━━━━━━━━━━━━━━━━━
//     * 小游戏相关
//     * ━━━━━━━━━━━━━━━━━━━
//     */
//    //你画我猜-发起邀请(包含音视频)
//    @FormUrlEncoded
//    @POST()
//    suspend fun launchGuessDraw(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<GuessDBean>
//
//    //你画我猜-发起邀请（不包含音视频）
//    @FormUrlEncoded
//    @POST()
//    suspend fun launchGuessDrawOnly(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<GuessDBean>
//
//    //你画我猜-接受邀请
//    @FormUrlEncoded
//    @POST()
//    suspend fun acceptGuessDraw(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<GuessDBean>
//
//    //你画我猜-拒绝邀请
//    @FormUrlEncoded
//    @POST()
//    suspend fun refuseGuessDraw(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<String>
//
//    //你画我猜-选择题目
//    @FormUrlEncoded
//    @POST()
//    suspend fun selectTopic(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<String>
//
//    //你画我猜-发送绘制数据
//    @FormUrlEncoded
//    @POST()
//    suspend fun sendDrawData(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<String>
//
//    //你画我猜-发送回合结束通知
//    @FormUrlEncoded
//    @POST()
//    suspend fun sendDrawRoundEnd(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<String>
//
//    //你画我猜-发送回合结束通知
//    @FormUrlEncoded
//    @POST()
//    suspend fun sendAnswer(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<String>
//
//    //你画我猜-退出游戏
//    @FormUrlEncoded
//    @POST()
//    suspend fun stopGuessd(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<String>
//
//    //你画我猜-删除最后一画
//    @FormUrlEncoded
//    @POST()
//    suspend fun removeLast(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<String>
//
//    /**
//     * ━━━━━━━━━━━━━━━━━━━
//     * 消息相关
//     * ━━━━━━━━━━━━━━━━━━━
//     */
//    //主页消息tab，顶部 视频匹配和今日推荐
//    @FormUrlEncoded
//    @POST()
//    suspend fun getMesTop(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<ArrayList<MesTopModel>>
//
//    //看过你的人
//    @FormUrlEncoded
//    @POST()
//    suspend fun getLookedList(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<ArrayList<LookedBean>>
//
//    /**
//     * ━━━━━━━━━━━━━━━━━━━
//     * 语聊房相关
//     * ━━━━━━━━━━━━━━━━━━━
//     */
//    //检测创建家族语聊房权限
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomCheck(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<CheckStartRoomBean>
//
//    //开启语聊房
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomStart(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<StartRoomBean>
//
//    //更新语聊房内容
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomUpdateInfo(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //获取语聊房房间信息
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomGetInfo(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<RoomInfoBean>
//
//    //关闭语聊房
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomClose(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<CloseRoomBean>
//
//    //收藏和取消收藏房间
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomFollow(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //获取语聊房家族提示信息
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomGetFamilyInfo(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<List<FamilyTagBean>>
//
//    //进入语聊房
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomEnter(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<EnterRoomBean>
//
//    //获取语聊房顶部在线列表
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomGetOnline(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<RoomOnlineBean>
//
//    //获取连麦申请列表
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomGetInviteList(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<SeatApplyListBean>
//
//    //获取语聊房围观群众列表
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomGetOnLookersList(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<OnLookersListBean>
//
//    //上麦申请
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomInviteToUpMic(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //允许上麦
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomAcceptUpMic(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //取消上麦申请
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomCancelUpMic(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //下麦
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomDownMic(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //抱下麦
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomRemoveSeat(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //获取在麦位用户列表
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomGetBroadcasters(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<AllSeatInfos>
//
//    //禁止座位
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomBanSeat(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //切换座位
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomSwitchSeat(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //管理员禁止用户麦克风状态
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomBanMic(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //自己切换麦克风状态
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomUpdateMicStatus(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //获取管理员列表信息
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomGetManagerList(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<RoomManageListBean>
//
//    //设置（取消）管理员
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomSetManager(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //踢人
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomKickUser(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //获取用户卡片信息
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomGetCardUserinfo(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<GiftCardInfoBean>
//
//    //发送文本消息
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomSendText(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //送礼
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomSendGift(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<GiftSend>
//
//    //获取榜单列表
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomGetRankList(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<RankBean>
//
//    //语聊房Banner
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomGetBanner(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<List<ActivityBean>>
//
//    //下一个房间
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomNextRec(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<EnterRoomBean>
//
//    //获取家族厅列表
//    @FormUrlEncoded
//    @POST()
//    suspend fun voiceRoomGetList(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<RoomListBean>
//
//    /**
//     * ━━━━━━━━━━━━━━━━━━━
//     * 家族相关
//     * ━━━━━━━━━━━━━━━━━━━
//     */
//    //家族创建-检测
//    @FormUrlEncoded
//    @POST()
//    suspend fun familyCreateCheck(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //家族创建
//    @FormUrlEncoded
//    @POST()
//    suspend fun familyCreate(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<CreateFamilySuccessBean>
//
//    //更新家族资料
//    @FormUrlEncoded
//    @POST()
//    suspend fun familyEdit(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //获取家族设置页信息
//    @FormUrlEncoded
//    @POST()
//    suspend fun getFamilySettingMes(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<FamilySettingMesBean>
//
//    //退出家族
//    @FormUrlEncoded
//    @POST()
//    suspend fun quitFamily(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //解散家族
//    @FormUrlEncoded
//    @POST()
//    suspend fun dissolutionFamily(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //获取家族申请列表
//    @FormUrlEncoded
//    @POST()
//    suspend fun getFamilyApplyList(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<ArrayList<FamilyApplyBean>>
//
//    //申请加入
//    @FormUrlEncoded
//    @POST()
//    suspend fun familyApply(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //对加入申请的操作 0忽略  1通过
//    @FormUrlEncoded
//    @POST()
//    suspend fun applyOperation(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //获取家族详情
//    @FormUrlEncoded
//    @POST()
//    suspend fun getFamilyDetail(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<FamilyDetailBean>
//
//    //获取群所有成员
//    @FormUrlEncoded
//    @POST()
//    suspend fun getMember(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<FamilyMemberResponse>
//
//    //获取好友列表
//    @FormUrlEncoded
//    @POST()
//    suspend fun getFriends(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<MFriendsBean>
//
//    //获取关注列表
//    @FormUrlEncoded
//    @POST()
//    suspend fun getFollowList(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<FollowListBean>
//
//    //获取粉丝列表
//    @FormUrlEncoded
//    @POST()
//    suspend fun getFans(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<FansBean>
//
//    //获取所有群用户uid （用于邀请好友是匹配uid判断是否已在群内）
//    @FormUrlEncoded
//    @POST()
//    suspend fun getAllUidInGroup(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<ArrayList<String>>
//
//    //邀请好友（泛指密友，关注，粉丝等）
//    @FormUrlEncoded
//    @POST()
//    suspend fun inviteFriends(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //设置/撤销身份
//    @FormUrlEncoded
//    @POST()
//    suspend fun setIdentity(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //家族广场-顶部数据
//    @FormUrlEncoded
//    @POST("/team/group/getRecommendGroupTopInfo")
//    suspend fun getFamilyHomePageTop(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<FamilyHomepageTopBean>
//
//    //家族广场-下面广场列表数据
//    @FormUrlEncoded
//    @POST("/team/group/getGroupPoolList")
//    suspend fun getFamilyHomePageSquareList(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<ArrayList<FamilyListBean>>
//
//    //家族广场-下面广场列表中的banner
//    @FormUrlEncoded
//    @POST()
//    suspend fun getFamilyHomePageSquareListBanner(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<BannerListBean>
//
//    //福袋-获取配置
//    @FormUrlEncoded
//    @POST()
//    suspend fun getRedBagConfig(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<ArrayList<FamilyRedBagBean>>
//
//    //福袋-获取list
//    @FormUrlEncoded
//    @POST()
//    suspend fun getRedBagList(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<ArrayList<FamilyRedBagListBean>>
//
//    //福袋-发送
//    @FormUrlEncoded
//    @POST()
//    suspend fun sendRedBag(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //福袋-领取
//    @FormUrlEncoded
//    @POST()
//    suspend fun receiveRedBag(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<FamilyRedBagReceivedBean>
//
//    //进入家族聊天
//    @FormUrlEncoded
//    @POST()
//    suspend fun getFamilyChatInfo(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<FamilyChatEnter>
//
//    //聊天检查
//    @FormUrlEncoded
//    @POST()
//    suspend fun familyChatCheck(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<FamilyChatCheck>
//
//    //退出聊天
//    @FormUrlEncoded
//    @POST()
//    suspend fun familyChatExit(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<Any>
//
//    //家族送礼
//    @FormUrlEncoded
//    @POST()
//    suspend fun familySendGift(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<GiftSend>
//
//    //家族签到
//    @FormUrlEncoded
//    @POST()
//    suspend fun familyTaskSign(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<FamilySigninBean>
//
//    //家族Banner
//    @FormUrlEncoded
//    @POST()
//    suspend fun familyGetBanner(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<List<ActivityBean>>
//
//    //家族任务状态
//    @FormUrlEncoded
//    @POST()
//    suspend fun familyGetTaskStatus(@Url url: String, @FieldMap maps: Map<String, String>): BaseResponse<FamilyChatTask>
//
//
//    /**
//     * ━━━━━━━━━━━━━━━━━━━
//     * lib_project里的，临时放在这
//     * ━━━━━━━━━━━━━━━━━━━
//     */
//    //获取充值弹层信息
//    @FormUrlEncoded
//    @POST("/user/getRecommendPayBag")
//    suspend fun getPayDialog(@FieldMap maps: Map<String, String>): BaseResponse<PayDialogBean>
////    @POST
////    fun postBody(@Url url: String?, @Body `object`: Any?): Observable<ResponseBody?>?
////
////    @POST
////    @Headers("Content-Type: application/json", "Accept: application/json")
////    fun postJson(@Url url: String?, @Body jsonBody: RequestBody?): Observable<ResponseBody?>?
////
//////    @POST
//////    fun postBody(@Url url: String?, @Body body: RequestBody?): BaseResponse<T>
////
////    @GET
////    operator fun get(@Url url: String?, @QueryMap maps: Map<String?, String?>?): Observable<ResponseBody?>?
////
////    @DELETE
////    fun delete(@Url url: String?, @QueryMap maps: Map<String?, String?>?): Observable<ResponseBody?>?
////
////    //@DELETE()//delete body请求比较特殊 需要自定义
////    @HTTP(method = "DELETE", hasBody = true)
////    fun deleteBody(@Url url: String?, @Body `object`: Any?): Observable<ResponseBody?>?
////
////    //@DELETE()//delete body请求比较特殊 需要自定义
////    @HTTP(method = "DELETE", hasBody = true)
////    fun deleteBody(@Url url: String?, @Body body: RequestBody?): Observable<ResponseBody?>?
////
////    //@DELETE()//delete body请求比较特殊 需要自定义
////    @Headers("Content-Type: application/json", "Accept: application/json")
////    @HTTP(method = "DELETE", hasBody = true)
////    fun deleteJson(@Url url: String?, @Body jsonBody: RequestBody?): Observable<ResponseBody?>?
////
////    @PUT
////    fun put(@Url url: String?, @QueryMap maps: Map<String?, String?>?): Observable<ResponseBody?>?
////
////    @PUT
////    fun putBody(@Url url: String?, @Body `object`: Any?): Observable<ResponseBody?>?
////
////    @PUT
////    fun putBody(@Url url: String?, @Body body: RequestBody?): Observable<ResponseBody?>?
////
////    @PUT
////    @Headers("Content-Type: application/json", "Accept: application/json")
////    fun putJson(@Url url: String?, @Body jsonBody: RequestBody?): Observable<ResponseBody?>?
////
////    @Multipart
////    @POST
////    fun uploadFlie(@Url fileUrl: String?, @Part("description") description: RequestBody?, @Part("files") file: MultipartBody.Part?): Observable<ResponseBody?>?
////
////    @Multipart
////    @POST
////    fun uploadFiles(@Url url: String?, @PartMap maps: Map<String?, RequestBody?>?): Observable<ResponseBody?>?
////
////    @Multipart
////    @POST
////    fun uploadFiles(@Url url: String?, @Part parts: List<MultipartBody.Part?>?): Observable<ResponseBody?>?
////
////    @Streaming
////    @GET
////    fun downloadFile(@Url fileUrl: String?): Observable<ResponseBody?>?
//
////    fun findMethod(url: String, params: Map<String, String>): BaseResponse<GuessDBean> {
////        var method = javaClass.getMethod("")
////        var urlAnnotation = method.getAnnotation(POST::class.java)
////        if (urlAnnotation != null) {
////            val urlAnnotationStr = urlAnnotation.value
////            if (urlAnnotationStr.isNotEmpty()) {
////                return method.invoke(this, url, params)
////            }
////        }
////        return null
////    }

}