<template>
  	<div :class="$style.frame">
    		<div :class="$style.body">
      			<div :class="$style.div">
        				<div :class="$style.header">
          					<div :class="$style.div2">
            						<div :class="$style.div3">
              							<img :class="$style.imgIcon" alt="" />
              							<div :class="$style.div4">
                								<div :class="$style.div5">안녕하세요!</div>
                								<div :class="$style.div6">김환자님</div>
              							</div>
            						</div>
            						<img :class="$style.buttonIcon" alt="" />
          					</div>
        				</div>
        				<div :class="$style.main">
          					<div :class="$style.section">
            						<div :class="$style.div7">
              							<div :class="$style.div8">
                								<div :class="$style.div9">오늘의 일정</div>
                								<img :class="$style.iIcon" alt="" />
              							</div>
              							<div :class="$style.div10">
                								<div :class="$style.div11">
                  									<div :class="$style.div12">
                    										<div :class="$style.div13">
                      											<div :class="$style.div14">정기 검진</div>
                      											<div :class="$style.div15">내과 - 김의사</div>
                    										</div>
                    										<div :class="$style.div16">
                      											<div :class="$style.div17">오후 2:30</div>
                      											<div :class="$style.div18">1시간 30분 후</div>
                    										</div>
                  									</div>
                								</div>
                								<div :class="$style.div19">
                  									<div :class="$style.div12">
                    										<div :class="$style.div21">
                      											<div :class="$style.div14">약물 복용</div>
                      											<div :class="$style.div23">혈압약 복용 시간</div>
                    										</div>
                    										<div :class="$style.div24">
                      											<div :class="$style.div25">오후 6:00</div>
                      											<div :class="$style.div26">5시간 후</div>
                    										</div>
                  									</div>
                								</div>
              							</div>
            						</div>
          					</div>
          					<div :class="$style.section2">
            						<div :class="$style.button">
              							<div :class="$style.div27">
                								<img :class="$style.divIcon" alt="" />
                								<div :class="$style.div28">기록</div>
              							</div>
            						</div>
            						<div :class="$style.button2">
              							<div :class="$style.div27">
                								<img :class="$style.divIcon" alt="" />
                								<div :class="$style.div30">비상연락</div>
              							</div>
            						</div>
          					</div>
          					<div :class="$style.button3">
            						<div :class="$style.div27">
              							<img :class="$style.divIcon" alt="" />
              							<div :class="$style.div32">내정보</div>
            						</div>
          					</div>
          					<div :class="$style.button4">
            						<div :class="$style.div27">
              							<img :class="$style.divIcon" alt="" />
              							<div :class="$style.ai">AI챗봇</div>
            						</div>
          					</div>
        				</div>
      			</div>
    		</div>
  	</div>
</template>
<style  module>.frame {
  	width: 100%;
  	position: relative;
  	border-radius: 8px;
  	background-color: #fff;
  	border: 2px solid #ced4da;
  	box-sizing: border-box;
  	height: 917px;
  	overflow: hidden;
  	display: flex;
  	flex-direction: column;
  	align-items: flex-start;
  	text-align: left;
  	font-size: 18px;
  	color: #111827;
  	font-family: 'Noto Sans KR';
}
  	.body {
    		align-self: stretch;
    		position: relative;
    		background-color: #f9fafb;
    		height: 917px;
  	}
  	.div {
    		position: absolute;
    		top: 0px;
    		left: 3px;
    		background-color: #fff;
    		width: 409px;
    		height: 917px;
  	}
  	.header {
    		position: absolute;
    		top: 0px;
    		left: 0px;
    		background-color: #fff;
    		border-bottom: 1px solid #f3f4f6;
    		box-sizing: border-box;
    		width: 384px;
    		height: 81px;
  	}
  	.div2 {
    		position: absolute;
    		top: 16px;
    		left: 24px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 336px;
    		height: 48px;
  	}
  	.div3 {
    		position: absolute;
    		top: 0px;
    		left: 0px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 141.2px;
    		height: 48px;
  	}
  	.imgIcon {
    		position: absolute;
    		top: 4px;
    		left: 0px;
    		border-radius: 9999px;
    		width: 40px;
    		height: 40px;
    		object-fit: cover;
  	}
  	.div4 {
    		position: absolute;
    		top: 0px;
    		left: 52px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 89.2px;
    		height: 48px;
  	}
  	.div5 {
    		position: absolute;
    		top: 0px;
    		left: 0px;
    		line-height: 28px;
    		display: inline-block;
    		width: 90px;
    		height: 28px;
  	}
  	.div6 {
    		position: absolute;
    		top: 28px;
    		left: 0px;
    		font-size: 14px;
    		line-height: 20px;
    		color: #4b5563;
    		display: inline-block;
    		width: 57px;
    		height: 20px;
  	}
  	.buttonIcon {
    		position: absolute;
    		top: 2px;
    		left: 304.25px;
    		border-radius: 9999px;
    		width: 31.8px;
    		height: 44px;
  	}
  	.main {
    		position: absolute;
    		top: 81px;
    		left: 12px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 397px;
    		height: 836px;
    		text-align: center;
    		font-size: 16px;
  	}
  	.section {
    		position: absolute;
    		top: 50px;
    		left: 24px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 336px;
    		height: 256px;
    		text-align: left;
    		font-size: 20px;
    		color: #fff;
  	}
  	.div7 {
    		position: absolute;
    		top: 0px;
    		left: 0px;
    		border-radius: 16px;
    		background: linear-gradient(90deg, #3b82f6, #2563eb);
    		width: 336px;
    		height: 256px;
  	}
  	.div8 {
    		position: absolute;
    		top: 24px;
    		left: 24px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 288px;
    		height: 28px;
  	}
  	.div9 {
    		position: absolute;
    		top: 0px;
    		left: 0px;
    		line-height: 28px;
    		display: inline-block;
    		width: 97px;
    		height: 29px;
  	}
  	.iIcon {
    		position: absolute;
    		top: 0px;
    		left: 270.5px;
    		width: 17.5px;
    		height: 28px;
  	}
  	.div10 {
    		position: absolute;
    		top: 68px;
    		left: 24px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 288px;
    		height: 164px;
    		font-size: 16px;
  	}
  	.div11 {
    		position: absolute;
    		top: 0px;
    		left: 0px;
    		border-radius: 8px;
    		background-color: rgba(255, 255, 255, 0.2);
    		width: 288px;
    		height: 76px;
  	}
  	.div12 {
    		position: absolute;
    		top: 16px;
    		left: 16px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 256px;
    		height: 44px;
  	}
  	.div13 {
    		position: absolute;
    		top: 0px;
    		left: 0px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 75.5px;
    		height: 44px;
  	}
  	.div14 {
    		position: absolute;
    		top: 0px;
    		left: 0px;
    		line-height: 24px;
    		display: inline-block;
    		width: 68px;
    		height: 24px;
  	}
  	.div15 {
    		position: absolute;
    		top: 24px;
    		left: 0px;
    		font-size: 14px;
    		line-height: 20px;
    		display: inline-block;
    		width: 76px;
    		height: 20px;
  	}
  	.div16 {
    		position: absolute;
    		top: 2px;
    		left: 186.47px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 69.5px;
    		height: 40px;
    		text-align: right;
  	}
  	.div17 {
    		position: absolute;
    		top: 0px;
    		left: 3.66px;
    		line-height: 24px;
    		display: inline-block;
    		width: 66px;
    		height: 24px;
  	}
  	.div18 {
    		position: absolute;
    		top: 24px;
    		left: 0px;
    		font-size: 12px;
    		line-height: 16px;
    		display: inline-block;
    		width: 70px;
    		height: 17px;
  	}
  	.div19 {
    		position: absolute;
    		top: 88px;
    		left: 0px;
    		border-radius: 8px;
    		background-color: rgba(255, 255, 255, 0.2);
    		width: 288px;
    		height: 76px;
  	}
  	.div21 {
    		position: absolute;
    		top: 0px;
    		left: 0px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 96.4px;
    		height: 44px;
  	}
  	.div23 {
    		position: absolute;
    		top: 24px;
    		left: 0px;
    		font-size: 14px;
    		line-height: 20px;
    		display: inline-block;
    		width: 97px;
    		height: 20px;
  	}
  	.div24 {
    		position: absolute;
    		top: 2px;
    		left: 190.13px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 65.9px;
    		height: 40px;
    		text-align: right;
  	}
  	.div25 {
    		position: absolute;
    		top: 0px;
    		left: 0px;
    		line-height: 24px;
    		display: inline-block;
    		width: 66px;
    		height: 24px;
  	}
  	.div26 {
    		position: absolute;
    		top: 24px;
    		left: 23.41px;
    		font-size: 12px;
    		line-height: 16px;
    		display: inline-block;
    		width: 48px;
    		height: 17px;
  	}
  	.section2 {
    		position: absolute;
    		top: 377px;
    		left: 24px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 336px;
    		height: 300px;
  	}
  	.button {
    		position: absolute;
    		top: 132px;
    		left: 0px;
    		box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.05);
    		border-radius: 16px;
    		background-color: #fff;
    		border: 1px solid #f3f4f6;
    		box-sizing: border-box;
    		width: 160px;
    		height: 142px;
  	}
  	.div27 {
    		position: absolute;
    		top: 25px;
    		left: 25px;
    		background-color: rgba(0, 0, 0, 0);
    		width: 110px;
    		height: 92px;
  	}
  	.divIcon {
    		position: absolute;
    		top: 0px;
    		left: 27px;
    		border-radius: 16px;
    		width: 56px;
    		height: 56px;
  	}
  	.div28 {
    		position: absolute;
    		top: 68px;
    		left: 40.27px;
    		line-height: 24px;
    		display: inline-block;
    		width: 30px;
    		height: 24px;
  	}
  	.button2 {
    		position: absolute;
    		top: 132px;
    		left: 176px;
    		box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.05);
    		border-radius: 16px;
    		background-color: #fff;
    		border: 1px solid #f3f4f6;
    		box-sizing: border-box;
    		width: 160px;
    		height: 142px;
  	}
  	.div30 {
    		position: absolute;
    		top: 68px;
    		left: 25.55px;
    		line-height: 24px;
    		display: inline-block;
    		width: 59px;
    		height: 24px;
  	}
  	.button3 {
    		position: absolute;
    		top: 347px;
    		left: 24px;
    		box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.05);
    		border-radius: 16px;
    		background-color: #fff;
    		border: 1px solid #f3f4f6;
    		box-sizing: border-box;
    		width: 160px;
    		height: 142px;
  	}
  	.div32 {
    		position: absolute;
    		top: 68px;
    		left: 32.91px;
    		line-height: 24px;
    		display: inline-block;
    		width: 45px;
    		height: 24px;
  	}
  	.button4 {
    		position: absolute;
    		top: 347px;
    		left: 200px;
    		box-shadow: 0px 1px 2px rgba(0, 0, 0, 0.05);
    		border-radius: 16px;
    		background-color: #fff;
    		border: 1px solid #f3f4f6;
    		box-sizing: border-box;
    		width: 160px;
    		height: 142px;
  	}
  	.ai {
    		position: absolute;
    		top: 68px;
    		left: 32.67px;
    		line-height: 24px;
    		display: inline-block;
    		width: 45px;
    		height: 24px;
  	}

</style>