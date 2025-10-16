<template>
    <div class="container-sm py-3" style="max-width:414px">
        <!-- 상단 헤더 -->
        <div class="d-flex align-items-center justify-content-between mb-2">
            <button class="btn btn-link text-body p-0"><i class="bi bi-chevron-left fs-5"></i></button>
            <div>
                <h6 class="mb-0 fw-semibold">보호자-환자 연결</h6>
                <small class="text-secondary">연결 유지 중</small>
            </div>
            <button class="btn btn-link text-body p-0"><i class="bi bi-three-dots-vertical fs-5"></i></button>
        </div>

        <!-- 연결된 환자 (1명 고정) -->
        <div class="card border-0 shadow-sm mb-3">
            <div class="card-body">
                <div class="d-flex align-items-center justify-content-between mb-2">
                    <div class="fw-semibold d-flex align-items-center gap-2">
                        <span class="rounded-circle d-inline-flex align-items-center justify-content-center"
                            style="width:40px;height:40px;background:#f1f3f5">
                            <i class="bi bi-person"></i>
                        </span>
                        연결된 환자
                    </div>
                    <button class="btn btn-danger btn-sm" disabled>
                        <i class="bi bi-link-slash me-1"></i>연결 해제
                    </button>
                </div>

                <h4 class="fw-bold mb-3">환자 정보</h4>

                <!-- 정보 라인들 -->
                <div class="border-top">
                    <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
                        <div class="text-secondary">환자 번호</div>
                        <div class="fw-semibold">#{{ patient.id }}</div>
                    </div>
                    <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
                        <div class="text-secondary">이름</div>
                        <div class="fw-semibold">{{ patient.name }}</div>
                    </div>
                    <div class="d-flex justify-content-between align-items-center py-2 border-bottom">
                        <div class="text-secondary">생년월일</div>
                        <div class="fw-semibold">{{ patient.birth }}</div>
                    </div>
                    <div class="d-flex justify-content-between align-items-center py-2">
                        <div class="text-secondary">연락처</div>
                        <div class="fw-semibold">{{ patient.phone }}</div>
                    </div>
                </div>
            </div>
        </div>

        <!-- 연결된 보호자 (구독권: 최대 3명) — 기존 구성 유지 -->
        <div class="d-flex align-items-center justify-content-between mb-2">
            <div class="fw-semibold"><i class="bi bi-people me-1"></i>연결된 보호자</div>
            <small class="text-secondary">최대 {{ subscriptionSlots }}명</small>
        </div>

        <div class="row row-cols-3 g-2 mb-3">
            <div v-for="(g, idx) in guardianSlots" :key="idx" class="col">
                <!-- 채워진 슬롯 -->
                <div v-if="g" class="card h-100 border">
                    <div class="card-body text-center p-2">
                        <div class="rounded-circle d-inline-flex align-items-center justify-content-center bg-light mb-1"
                            style="width:38px;height:38px">
                            <i class="bi bi-person-circle"></i>
                        </div>
                        <div class="small fw-semibold text-truncate">{{ g.name }}</div>
                        <div class="text-secondary small text-truncate">{{ g.phone }}</div>
                        <span class="badge bg-light text-secondary mt-1">
                            <i class="bi bi-shield-check me-1"></i>{{ g.role }}
                        </span>
                    </div>
                </div>

                <!-- 빈 슬롯 -->
                <div v-else class="card h-100 border bg-light-subtle">
                    <div class="card-body text-center p-2 d-flex flex-column align-items-center justify-content-center">
                        <div class="rounded-circle d-inline-flex align-items-center justify-content-center mb-1 bg-white border"
                            style="width:38px;height:38px">
                            <i class="bi bi-person-plus"></i>
                        </div>
                        <div class="small text-secondary">빈 슬롯</div>
                        <button class="btn btn-sm btn-outline-secondary mt-1" disabled>초대</button>
                    </div>
                </div>
            </div>
        </div>

        <!-- 안내 -->
        <p class="text-success small mb-0">저장된 연결을 불러왔습니다.</p>
        <div class="py-2"></div>
    </div>
</template>

<script setup>
import { ref } from 'vue'

// 1명 고정 환자 (데모)
const patient = ref({
    id: 20,
    name: '이주형',
    birth: '2025-10-03',
    phone: '010-1234-5678'
})

// 구독권: 보호자 최대 3명 (기존 그대로)
const subscriptionSlots = 3
const guardianSlots = ref([
    { name: '김보호자', phone: '010-2222-3333', role: '관리자' },
    { name: '나 (부보호자)', phone: '010-4444-5555', role: '부관리자' },
    null // 빈 슬롯
])
</script>
