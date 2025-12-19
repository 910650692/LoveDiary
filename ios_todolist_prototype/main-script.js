// ===== 全局变量 =====
let currentPage = 'home';
let isAnimating = false;

// ===== 应用初始化 =====
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
});

function initializeApp() {
    // 显示启动页面
    showSplashScreen();
    
    // 2秒后隐藏启动页面并显示主应用
    setTimeout(() => {
        hideSplashScreen();
        showMainApp();
    }, 2000);
    
    // 初始化事件监听器
    setupEventListeners();
    
    // 初始化手势支持
    setupGestureSupport();
    
    // 更新状态栏时间
    updateStatusBarTime();
    setInterval(updateStatusBarTime, 60000); // 每分钟更新一次
}

// ===== 启动页面控制 =====
function showSplashScreen() {
    const splashScreen = document.getElementById('splashScreen');
    splashScreen.style.display = 'flex';
}

function hideSplashScreen() {
    const splashScreen = document.getElementById('splashScreen');
    splashScreen.classList.add('hidden');
    
    setTimeout(() => {
        splashScreen.style.display = 'none';
    }, 500);
}

function showMainApp() {
    const appContainer = document.getElementById('appContainer');
    appContainer.classList.add('show');
}

// ===== 页面导航 =====
function navigateToTab(tabName) {
    if (isAnimating || currentPage === tabName) return;
    
    isAnimating = true;
    
    // 更新tab bar状态
    updateTabBar(tabName);
    
    // 切换页面
    switchPage(tabName);
    
    // 添加触觉反馈
    addHapticFeedback();
    
    setTimeout(() => {
        isAnimating = false;
    }, 400);
}

function updateTabBar(activeTab) {
    const tabItems = document.querySelectorAll('.tab-item');
    
    tabItems.forEach(item => {
        item.classList.remove('active');
        if (item.dataset.page === activeTab) {
            item.classList.add('active');
        }
    });
}

function switchPage(pageName) {
    const currentPageElement = document.querySelector('.page.active');
    const targetPageElement = document.getElementById(pageName + 'Page');
    
    if (!targetPageElement) {
        console.error(`Page ${pageName} not found`);
        return;
    }
    
    // 隐藏当前页面
    if (currentPageElement) {
        currentPageElement.classList.remove('active');
    }
    
    // 显示目标页面
    targetPageElement.classList.add('active');
    
    // 更新当前页面状态
    currentPage = pageName;
    
    // 页面切换动画
    targetPageElement.style.transform = 'translateX(100%)';
    targetPageElement.style.opacity = '0';
    
    requestAnimationFrame(() => {
        targetPageElement.style.transition = 'transform 0.3s ease, opacity 0.3s ease';
        targetPageElement.style.transform = 'translateX(0)';
        targetPageElement.style.opacity = '1';
        
        setTimeout(() => {
            targetPageElement.style.transition = '';
            targetPageElement.style.transform = '';
            targetPageElement.style.opacity = '';
        }, 300);
    });
}

// ===== 事件监听器设置 =====
function setupEventListeners() {
    // Tab点击事件
    document.querySelectorAll('.tab-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const tabName = item.dataset.page;
            navigateToTab(tabName);
        });
    });
    
    // 快捷功能点击事件
    document.querySelectorAll('.action-item').forEach(item => {
        item.addEventListener('click', (e) => {
            addRippleEffect(e, item);
        });
    });
    
    // 按钮点击事件
    document.querySelectorAll('.add-btn, .ios-button').forEach(button => {
        button.addEventListener('click', (e) => {
            addButtonPressEffect(button);
        });
    });
    
    // 阻止默认的touch行为，提供更好的iOS体验
    document.addEventListener('touchstart', function(e) {
        if (e.target.closest('.tab-item, .action-item, .add-btn')) {
            e.preventDefault();
        }
    }, { passive: false });
}

// ===== 手势支持 =====
function setupGestureSupport() {
    let startX = 0;
    let startY = 0;
    let threshold = 50; // 滑动阈值
    
    document.addEventListener('touchstart', function(e) {
        startX = e.touches[0].clientX;
        startY = e.touches[0].clientY;
    });
    
    document.addEventListener('touchend', function(e) {
        if (!startX || !startY) return;
        
        let endX = e.changedTouches[0].clientX;
        let endY = e.changedTouches[0].clientY;
        
        let diffX = startX - endX;
        let diffY = startY - endY;
        
        // 检测是否为水平滑动且滑动距离超过阈值
        if (Math.abs(diffX) > Math.abs(diffY) && Math.abs(diffX) > threshold) {
            if (diffX > 0) {
                // 向左滑动 - 切换到下一个tab
                navigateToNextTab();
            } else {
                // 向右滑动 - 切换到上一个tab
                navigateToPrevTab();
            }
        }
        
        startX = 0;
        startY = 0;
    });
}

function navigateToNextTab() {
    const tabs = ['home', 'anniversary', 'album', 'todos', 'profile'];
    const currentIndex = tabs.indexOf(currentPage);
    const nextIndex = (currentIndex + 1) % tabs.length;
    navigateToTab(tabs[nextIndex]);
}

function navigateToPrevTab() {
    const tabs = ['home', 'anniversary', 'album', 'todos', 'profile'];
    const currentIndex = tabs.indexOf(currentPage);
    const prevIndex = (currentIndex - 1 + tabs.length) % tabs.length;
    navigateToTab(tabs[prevIndex]);
}

// ===== 视觉效果 =====
function addRippleEffect(event, element) {
    const rect = element.getBoundingClientRect();
    const ripple = document.createElement('div');
    
    const size = Math.max(rect.width, rect.height);
    const x = event.clientX - rect.left - size / 2;
    const y = event.clientY - rect.top - size / 2;
    
    ripple.style.cssText = `
        position: absolute;
        width: ${size}px;
        height: ${size}px;
        left: ${x}px;
        top: ${y}px;
        background: rgba(255, 107, 157, 0.3);
        border-radius: 50%;
        transform: scale(0);
        animation: rippleAnimation 0.6s ease-out;
        pointer-events: none;
        z-index: 1000;
    `;
    
    element.style.position = 'relative';
    element.style.overflow = 'hidden';
    element.appendChild(ripple);
    
    setTimeout(() => {
        ripple.remove();
    }, 600);
}

function addButtonPressEffect(button) {
    button.style.transform = 'scale(0.95)';
    button.style.boxShadow = '0 2px 8px rgba(255, 107, 157, 0.2)';
    
    setTimeout(() => {
        button.style.transform = '';
        button.style.boxShadow = '';
    }, 150);
}

function addHapticFeedback() {
    // 模拟触觉反馈 - 在真实设备上可以使用navigator.vibrate()
    if (navigator.vibrate) {
        navigator.vibrate(10);
    }
}

// ===== 相册相关功能 =====
let isSelectionMode = false;
let selectedPhotos = new Set();
let currentView = 'grid';

const photos = [
    { id: 1, src: 'photo1.jpg', date: '7月20日', favorite: false },
    { id: 2, src: 'photo2.jpg', date: '7月19日', favorite: true },
    { id: 3, src: 'photo3.jpg', date: '7月18日', favorite: false },
    { id: 4, src: 'photo4.jpg', date: '7月17日', favorite: false },
    { id: 5, src: 'photo5.jpg', date: '7月16日', favorite: true },
    { id: 6, src: 'photo6.jpg', date: '7月15日', favorite: false }
];

function toggleSelectMode() {
    isSelectionMode = !isSelectionMode;
    const selectBtn = document.querySelector('.select-btn');
    const toolbar = document.getElementById('selectionToolbar');
    const photosGrid = document.getElementById('photosGrid');
    
    if (isSelectionMode) {
        selectBtn.classList.add('active');
        toolbar.classList.add('active');
        photosGrid.classList.add('selection-mode');
        showToast('选择模式已开启');
    } else {
        selectBtn.classList.remove('active');
        toolbar.classList.remove('active');
        photosGrid.classList.remove('selection-mode');
        selectedPhotos.clear();
        updateSelectedCount();
        clearAllSelections();
        showToast('选择模式已关闭');
    }
    
    addHapticFeedback();
}

function switchView(view) {
    currentView = view;
    const toggleBtns = document.querySelectorAll('.toggle-btn');
    const photosGrid = document.getElementById('photosGrid');
    
    // 更新按钮状态
    toggleBtns.forEach(btn => {
        btn.classList.remove('active');
        if (btn.dataset.view === view) {
            btn.classList.add('active');
        }
    });
    
    // 切换视图
    if (view === 'list') {
        photosGrid.classList.add('list-view');
        showToast('列表视图');
    } else {
        photosGrid.classList.remove('list-view');
        showToast('网格视图');
    }
    
    addHapticFeedback();
}

function togglePhotoSelection(photoId, event) {
    event.stopPropagation();
    
    if (!isSelectionMode) return;
    
    const checkbox = document.getElementById(`photo-${photoId}`);
    
    if (checkbox.checked) {
        selectedPhotos.add(photoId);
    } else {
        selectedPhotos.delete(photoId);
    }
    
    updateSelectedCount();
    addHapticFeedback();
}

function updateSelectedCount() {
    const countElement = document.getElementById('selectedCount');
    if (countElement) {
        countElement.textContent = selectedPhotos.size;
    }
}

function selectAll() {
    if (!isSelectionMode) return;
    
    const checkboxes = document.querySelectorAll('.selection-checkbox input[type="checkbox"]');
    const allSelected = Array.from(checkboxes).every(cb => cb.checked);
    
    checkboxes.forEach(checkbox => {
        const photoId = parseInt(checkbox.id.split('-')[1]);
        checkbox.checked = !allSelected;
        
        if (!allSelected) {
            selectedPhotos.add(photoId);
        } else {
            selectedPhotos.delete(photoId);
        }
    });
    
    updateSelectedCount();
    showToast(allSelected ? '已取消全选' : `已选择全部 ${checkboxes.length} 张照片`);
    addHapticFeedback();
}

function clearAllSelections() {
    const checkboxes = document.querySelectorAll('.selection-checkbox input[type="checkbox"]');
    checkboxes.forEach(checkbox => {
        checkbox.checked = false;
    });
}

function deleteSelected() {
    if (selectedPhotos.size === 0) {
        showToast('请先选择要删除的照片');
        return;
    }
    
    if (confirm(`确定要删除选中的 ${selectedPhotos.size} 张照片吗？此操作无法撤销。`)) {
        // 这里可以添加实际的删除逻辑
        showToast(`已删除 ${selectedPhotos.size} 张照片`);
        
        // 删除选中的照片元素
        selectedPhotos.forEach(photoId => {
            const photoElement = document.querySelector(`[data-id="${photoId}"]`);
            if (photoElement) {
                photoElement.style.animation = 'fadeOut 0.3s ease';
                setTimeout(() => {
                    photoElement.remove();
                }, 300);
            }
        });
        
        selectedPhotos.clear();
        updateSelectedCount();
        addHapticFeedback();
    }
}

function addToFavorites() {
    if (selectedPhotos.size === 0) {
        showToast('请先选择要收藏的照片');
        return;
    }
    
    selectedPhotos.forEach(photoId => {
        const photo = photos.find(p => p.id === photoId);
        if (photo) {
            photo.favorite = true;
        }
        
        // 更新UI
        const favoriteBtn = document.querySelector(`[onclick="toggleFavorite(${photoId}, event)"]`);
        if (favoriteBtn) {
            favoriteBtn.classList.add('active');
        }
    });
    
    showToast(`已将 ${selectedPhotos.size} 张照片添加到收藏`);
    selectedPhotos.clear();
    updateSelectedCount();
    addHapticFeedback();
}

function toggleFavorite(photoId, event) {
    event.stopPropagation();
    
    const photo = photos.find(p => p.id === photoId);
    const favoriteBtn = event.target.closest('.favorite');
    
    if (photo) {
        photo.favorite = !photo.favorite;
        
        if (photo.favorite) {
            favoriteBtn.classList.add('active');
            showToast('已添加到收藏');
        } else {
            favoriteBtn.classList.remove('active');
            showToast('已取消收藏');
        }
        
        addHapticFeedback();
    }
}

function openPhoto(photoId) {
    if (isSelectionMode) {
        // 在选择模式下，点击照片应该切换选择状态
        const checkbox = document.getElementById(`photo-${photoId}`);
        checkbox.checked = !checkbox.checked;
        togglePhotoSelection(photoId, { stopPropagation: () => {} });
        return;
    }
    
    // 这里可以添加打开照片详情的逻辑
    showToast(`打开照片 ${photoId}`);
    addHapticFeedback();
}

function showAlbumCategory(category) {
    const categoryNames = {
        'all': '全部照片',
        'favorites': '我的收藏',
        'dates': '约会时光',
        'travel': '旅行记忆'
    };
    
    showToast(`查看${categoryNames[category] || category}`);
    addHapticFeedback();
}

// ===== 纪念日相关功能 =====
let currentDate = new Date();
let anniversaries = [
    { id: 1, title: '小仙女的生日', date: '2025-07-28', type: 'birthday', notificationEnabled: true },
    { id: 2, title: '恋爱1000天', date: '2025-08-14', type: 'love', notificationEnabled: false },
    { id: 3, title: '恋爱纪念日', date: '2025-01-14', type: 'special', notificationEnabled: true },
    { id: 4, title: '情人节', date: '2025-02-14', type: 'valentine', notificationEnabled: true }
];

function initializeCalendar() {
    generateCalendar();
    setupFilterTabs();
}

function generateCalendar() {
    const calendarBody = document.getElementById('calendarBody');
    const calendarTitle = document.getElementById('calendarTitle');
    
    if (!calendarBody || !calendarTitle) return;
    
    const year = currentDate.getFullYear();
    const month = currentDate.getMonth();
    
    // 更新标题
    calendarTitle.textContent = `${year}年${month + 1}月`;
    
    // 获取月份信息
    const firstDay = new Date(year, month, 1);
    const lastDay = new Date(year, month + 1, 0);
    const startDate = new Date(firstDay);
    startDate.setDate(startDate.getDate() - firstDay.getDay());
    
    // 清空日历
    calendarBody.innerHTML = '';
    
    // 生成6周的日历
    for (let week = 0; week < 6; week++) {
        for (let day = 0; day < 7; day++) {
            const currentCalendarDate = new Date(startDate);
            currentCalendarDate.setDate(startDate.getDate() + week * 7 + day);
            
            const dayElement = document.createElement('div');
            dayElement.className = 'calendar-day';
            dayElement.textContent = currentCalendarDate.getDate();
            
            // 添加样式类
            if (currentCalendarDate.getMonth() !== month) {
                dayElement.classList.add('other-month');
            }
            
            if (isToday(currentCalendarDate)) {
                dayElement.classList.add('today');
            }
            
            if (hasAnniversary(currentCalendarDate)) {
                dayElement.classList.add('has-anniversary');
            }
            
            // 添加点击事件
            dayElement.addEventListener('click', () => {
                selectCalendarDay(currentCalendarDate);
            });
            
            calendarBody.appendChild(dayElement);
        }
    }
}

function isToday(date) {
    const today = new Date();
    return date.toDateString() === today.toDateString();
}

function hasAnniversary(date) {
    const dateStr = date.toISOString().split('T')[0];
    return anniversaries.some(anniversary => anniversary.date === dateStr);
}

function selectCalendarDay(date) {
    const dateStr = date.toISOString().split('T')[0];
    const dayAnniversaries = anniversaries.filter(anniversary => anniversary.date === dateStr);
    
    if (dayAnniversaries.length > 0) {
        const titles = dayAnniversaries.map(a => a.title).join(', ');
        showToast(`这一天有纪念日：${titles}`);
    } else {
        showToast(`${date.getMonth() + 1}月${date.getDate()}日`);
    }
}

function prevMonth() {
    currentDate.setMonth(currentDate.getMonth() - 1);
    generateCalendar();
    addHapticFeedback();
}

function nextMonth() {
    currentDate.setMonth(currentDate.getMonth() + 1);
    generateCalendar();
    addHapticFeedback();
}

function setupFilterTabs() {
    const filterTabs = document.querySelectorAll('.filter-tab');
    filterTabs.forEach(tab => {
        tab.addEventListener('click', (e) => {
            // 更新active状态
            filterTabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            
            // 筛选纪念日
            const filter = tab.dataset.filter;
            filterAnniversaries(filter);
            
            addHapticFeedback();
        });
    });
}

function filterAnniversaries(filter) {
    const anniversaryCards = document.querySelectorAll('.anniversary-card');
    const today = new Date();
    
    anniversaryCards.forEach(card => {
        const cardDate = new Date(card.dataset.date);
        let show = true;
        
        switch (filter) {
            case 'upcoming':
                show = cardDate >= today;
                break;
            case 'passed':
                show = cardDate < today;
                break;
            case 'all':
            default:
                show = true;
                break;
        }
        
        if (show) {
            card.style.display = 'flex';
            card.style.animation = 'fadeInScale 0.3s ease';
        } else {
            card.style.display = 'none';
        }
    });
}

function editAnniversary(id) {
    const anniversary = anniversaries.find(a => a.id === id);
    if (anniversary) {
        showToast(`编辑纪念日：${anniversary.title}`);
        // 这里可以添加编辑模态框的逻辑
    }
}

function toggleNotification(id) {
    const anniversary = anniversaries.find(a => a.id === id);
    if (anniversary) {
        anniversary.notificationEnabled = !anniversary.notificationEnabled;
        
        // 更新UI
        const card = document.querySelector(`[onclick="toggleNotification(${id})"]`);
        const icon = card.querySelector('i');
        
        if (anniversary.notificationEnabled) {
            icon.className = 'fas fa-bell';
            showToast(`已开启 ${anniversary.title} 的提醒`);
        } else {
            icon.className = 'fas fa-bell-slash';
            showToast(`已关闭 ${anniversary.title} 的提醒`);
        }
        
        addHapticFeedback();
    }
}

// 在页面切换到纪念日时初始化日历
const originalNavigateToTab = navigateToTab;
navigateToTab = function(tabName) {
    originalNavigateToTab(tabName);
    
    if (tabName === 'anniversary') {
        setTimeout(() => {
            initializeCalendar();
        }, 100);
    }
};

// ===== 功能函数 =====
function showProfile() {
    navigateToTab('profile');
    showToast('个人中心');
}

function showAddAnniversary() {
    showToast('添加纪念日功能开发中');
}

function showAddPhoto() {
    showToast('添加照片功能开发中');
}

function showAddTodo() {
    showToast('添加愿望功能开发中');
}

function showAddPeriod() {
    showToast('添加生理期记录功能开发中');
}

function showToast(message, duration = 2000) {
    const toast = document.createElement('div');
    toast.className = 'toast';
    toast.innerHTML = `
        <i class="fas fa-info-circle"></i>
        <span>${message}</span>
    `;
    
    toast.style.cssText = `
        position: fixed;
        top: 100px;
        left: 50%;
        transform: translateX(-50%);
        background: rgba(0, 0, 0, 0.8);
        color: white;
        padding: 12px 20px;
        border-radius: 20px;
        font-size: 14px;
        font-weight: 500;
        z-index: 3000;
        display: flex;
        align-items: center;
        gap: 8px;
        backdrop-filter: blur(10px);
        animation: toastSlideIn 0.3s ease;
        max-width: 300px;
        text-align: center;
    `;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'toastSlideOut 0.3s ease';
        setTimeout(() => {
            toast.remove();
        }, 300);
    }, duration);
}

function updateStatusBarTime() {
    const timeElement = document.querySelector('.status-bar .time');
    const now = new Date();
    const timeString = now.toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit',
        hour12: false 
    });
    timeElement.textContent = timeString;
}

// ===== 数据模拟 =====
function loadUserData() {
    // 模拟加载用户数据
    return {
        couple: {
            female: {
                name: '小仙女',
                avatar: 'https://via.placeholder.com/60x60/FF6B9D/FFFFFF?text=👸'
            },
            male: {
                name: '白马王子',
                avatar: 'https://via.placeholder.com/60x60/87CEEB/FFFFFF?text=🤴'
            },
            loveStartDate: '2023-01-14',
            loveDays: 520
        },
        recentActivities: [
            {
                icon: '💕',
                text: '完成了愿望"一起看电影"',
                time: '2小时前'
            },
            {
                icon: '📸',
                text: '上传了3张新照片',
                time: '昨天'
            },
            {
                icon: '🎂',
                text: '距离生日还有7天',
                time: '即将到来'
            }
        ]
    };
}

// ===== CSS动画样式注入 =====
function injectAnimationStyles() {
    const style = document.createElement('style');
    style.textContent = `
        @keyframes rippleAnimation {
            0% {
                transform: scale(0);
                opacity: 1;
            }
            100% {
                transform: scale(4);
                opacity: 0;
            }
        }
        
        @keyframes toastSlideIn {
            from {
                opacity: 0;
                transform: translateX(-50%) translateY(-20px);
            }
            to {
                opacity: 1;
                transform: translateX(-50%) translateY(0);
            }
        }
        
        @keyframes toastSlideOut {
            from {
                opacity: 1;
                transform: translateX(-50%) translateY(0);
            }
            to {
                opacity: 0;
                transform: translateX(-50%) translateY(-20px);
            }
        }
        
        .toast {
            pointer-events: none;
        }
        
        /* 邀请码模态框样式 */
        .invite-modal-content {
            background: white;
            border-radius: 20px;
            max-width: 320px;
            width: 90%;
            box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
            animation: modalSlideUp 0.3s ease;
        }
        
        .invite-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 20px 20px 0;
            border-bottom: 1px solid #f0f0f0;
            margin-bottom: 20px;
        }
        
        .invite-header h3 {
            font-size: 18px;
            font-weight: 700;
            color: #333;
            margin: 0;
        }
        
        .close-btn {
            background: none;
            border: none;
            font-size: 24px;
            color: #999;
            cursor: pointer;
            width: 32px;
            height: 32px;
            display: flex;
            align-items: center;
            justify-content: center;
            border-radius: 50%;
            transition: background 0.2s ease;
        }
        
        .close-btn:hover {
            background: #f0f0f0;
        }
        
        .invite-body {
            padding: 0 20px 20px;
            text-align: center;
        }
        
        .invite-icon {
            font-size: 48px;
            margin-bottom: 16px;
        }
        
        .invite-text {
            font-size: 14px;
            color: #666;
            margin-bottom: 20px;
        }
        
        .invite-code {
            font-size: 24px;
            font-weight: 800;
            color: #FF6B9D;
            background: rgba(255, 107, 157, 0.1);
            padding: 16px;
            border-radius: 12px;
            margin-bottom: 20px;
            letter-spacing: 4px;
            font-family: 'Courier New', monospace;
        }
        
        .invite-actions {
            display: flex;
            gap: 12px;
        }
        
        .copy-btn, .share-btn {
            flex: 1;
            padding: 12px 16px;
            border: none;
            border-radius: 12px;
            font-size: 14px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.2s ease;
            display: flex;
            align-items: center;
            justify-content: center;
            gap: 6px;
        }
        
        .copy-btn {
            background: #FF6B9D;
            color: white;
        }
        
        .copy-btn:hover {
            background: #E5568A;
        }
        
        .share-btn {
            background: #f0f0f0;
            color: #333;
        }
        
        .share-btn:hover {
            background: #e0e0e0;
        }
        
        @keyframes modalSlideUp {
            from {
                opacity: 0;
                transform: translateY(50px) scale(0.9);
            }
            to {
                opacity: 1;
                transform: translateY(0) scale(1);
            }
        }
        
        /* iOS风格滚动条 */
        ::-webkit-scrollbar {
            width: 3px;
        }
        
        ::-webkit-scrollbar-track {
            background: transparent;
        }
        
        ::-webkit-scrollbar-thumb {
            background: rgba(255, 107, 157, 0.3);
            border-radius: 2px;
        }
        
        ::-webkit-scrollbar-thumb:hover {
            background: rgba(255, 107, 157, 0.5);
        }
        
        /* 选择文本的颜色 */
        ::selection {
            background: rgba(255, 107, 157, 0.2);
            color: var(--gray-900);
        }
        
        /* 去除点击高亮 */
        * {
            -webkit-tap-highlight-color: transparent;
            -webkit-touch-callout: none;
            -webkit-user-select: none;
            user-select: none;
        }
        
        input, textarea {
            -webkit-user-select: text;
            user-select: text;
        }
    `;
    document.head.appendChild(style);
}

// 在页面加载时注入样式
document.addEventListener('DOMContentLoaded', injectAnimationStyles);

function showSettings() {
    showToast('系统设置');
}

function editAvatar() {
    showToast('头像编辑功能开发中');
}

function showInviteCode() {
    // 生成随机邀请码
    const inviteCode = generateInviteCode();
    showInviteModal(inviteCode);
}

function showCoupleSettings() {
    showToast('情侣设置功能开发中');
}

function showPersonalSettings() {
    showToast('个人设置功能开发中');
}

function showNotificationSettings() {
    showToast('通知设置功能开发中');
}

function showDataSync() {
    showToast('数据同步功能开发中');
}

function showHelp() {
    showToast('帮助中心功能开发中');
}

function showAbout() {
    showToast('关于我们功能开发中');
}

function showLogoutConfirm() {
    if (confirm('确定要退出登录吗？')) {
        showToast('退出登录成功');
        // 这里可以添加实际的登出逻辑
    }
}

function generateInviteCode() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789';
    let result = '';
    for (let i = 0; i < 6; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

function showInviteModal(code) {
    const modal = document.createElement('div');
    modal.className = 'invite-modal';
    modal.innerHTML = `
        <div class="invite-modal-content">
            <div class="invite-header">
                <h3>邀请TA加入</h3>
                <button class="close-btn" onclick="closeInviteModal()">×</button>
            </div>
            <div class="invite-body">
                <div class="invite-icon">💕</div>
                <p class="invite-text">分享邀请码给你的恋人</p>
                <div class="invite-code">${code}</div>
                <div class="invite-actions">
                    <button class="copy-btn" onclick="copyInviteCode('${code}')">
                        <i class="fas fa-copy"></i>
                        复制邀请码
                    </button>
                    <button class="share-btn" onclick="shareInviteCode('${code}')">
                        <i class="fas fa-share"></i>
                        分享
                    </button>
                </div>
            </div>
        </div>
    `;
    
    modal.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 3000;
        animation: fadeIn 0.3s ease;
    `;
    
    document.body.appendChild(modal);
    window.currentInviteModal = modal;
}

function closeInviteModal() {
    if (window.currentInviteModal) {
        window.currentInviteModal.style.animation = 'fadeOut 0.3s ease';
        setTimeout(() => {
            window.currentInviteModal.remove();
            window.currentInviteModal = null;
        }, 300);
    }
}

function copyInviteCode(code) {
    if (navigator.clipboard) {
        navigator.clipboard.writeText(code).then(() => {
            showToast('邀请码已复制到剪贴板');
        });
    } else {
        // 兼容性处理
        const textArea = document.createElement('textarea');
        textArea.value = code;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        showToast('邀请码已复制');
    }
}

function shareInviteCode(code) {
    if (navigator.share) {
        navigator.share({
            title: '恋爱日记邀请',
            text: `我邀请你加入恋爱日记，一起记录我们的美好时光！邀请码：${code}`,
            url: window.location.href
        });
    } else {
        showToast('分享功能暂不支持');
    }
}

// ===== 导出函数供HTML调用 =====
window.navigateToTab = navigateToTab;
window.showProfile = showProfile;
window.showAddAnniversary = showAddAnniversary;
window.showAddPhoto = showAddPhoto;
window.showAddTodo = showAddTodo;
window.showAddPeriod = showAddPeriod;
window.showSettings = showSettings;
window.editAvatar = editAvatar;
window.showInviteCode = showInviteCode;
window.showCoupleSettings = showCoupleSettings;
window.showPersonalSettings = showPersonalSettings;
window.showNotificationSettings = showNotificationSettings;
window.showDataSync = showDataSync;
window.showHelp = showHelp;
window.showAbout = showAbout;
window.showLogoutConfirm = showLogoutConfirm;
window.closeInviteModal = closeInviteModal;
window.copyInviteCode = copyInviteCode;
window.shareInviteCode = shareInviteCode;
window.prevMonth = prevMonth;
window.nextMonth = nextMonth;
window.editAnniversary = editAnniversary;
window.toggleNotification = toggleNotification;
window.toggleSelectMode = toggleSelectMode;
window.switchView = switchView;
window.togglePhotoSelection = togglePhotoSelection;
window.selectAll = selectAll;
window.deleteSelected = deleteSelected;
window.addToFavorites = addToFavorites;
window.toggleFavorite = toggleFavorite;
window.openPhoto = openPhoto;
window.showAlbumCategory = showAlbumCategory;

// ===== 情绪记录相关函数 =====
let currentMoodMonth = new Date();

// 记录我的情绪
function recordMyMood() {
    showToast('情绪记录功能 - 打开情绪选择面板');
    // 实际开发时打开情绪选择模态框
}

// 查看对方情绪
function viewPartnerMood() {
    showToast('查看Ta的详细情绪记录');
}

// 安慰对方
function comfortPartner(event) {
    if (event) {
        event.stopPropagation();
    }
    showToast('💕 给Ta发送一个拥抱~');
    // 实际开发时打开评论/留言框
}

// 显示记录情绪面板
function showRecordMood() {
    showToast('打开情绪记录面板 - 选择今天的心情');
    // 实际开发时显示情绪选择器（开心/难过/生气等）
}

// 切换情绪日历月份
function prevMoodMonth() {
    currentMoodMonth.setMonth(currentMoodMonth.getMonth() - 1);
    updateMoodCalendar();
    showToast(`切换到 ${currentMoodMonth.getFullYear()}年${currentMoodMonth.getMonth() + 1}月`);
}

function nextMoodMonth() {
    currentMoodMonth.setMonth(currentMoodMonth.getMonth() + 1);
    updateMoodCalendar();
    showToast(`切换到 ${currentMoodMonth.getFullYear()}年${currentMoodMonth.getMonth() + 1}月`);
}

function updateMoodCalendar() {
    const titleElement = document.getElementById('moodCalendarTitle');
    if (titleElement) {
        titleElement.textContent = `${currentMoodMonth.getFullYear()}年${currentMoodMonth.getMonth() + 1}月`;
    }
    // 实际开发时重新渲染日历
}

// 查看某天的情绪详情
function viewDayMood(date) {
    showToast(`查看 ${date} 的情绪记录`);
    // 实际开发时显示该日期的详细情绪
}

// ===== 愿望清单相关函数 =====
let todosData = [
    { id: 1, title: '一起去看极光', status: 'pending', priority: 'high' },
    { id: 2, title: '一起做一顿大餐', status: 'completed', priority: 'medium' },
    { id: 3, title: '去迪士尼乐园', status: 'pending', priority: 'medium' },
    { id: 4, title: '学会一首双人舞', status: 'pending', priority: 'low' }
];

// 显示添加愿望面板
function showAddTodo() {
    showToast('打开添加愿望面板');
    // 实际开发时显示添加表单
}

// 切换愿望状态
function toggleTodoStatus(todoId) {
    const todo = todosData.find(t => t.id === todoId);
    if (todo) {
        todo.status = todo.status === 'pending' ? 'completed' : 'pending';

        if (todo.status === 'completed') {
            showToast('✨ 恭喜完成一个愿望！');
            // 显示庆祝动画
            triggerCelebration();
        } else {
            showToast('愿望标记为未完成');
        }

        // 更新UI
        updateTodoCard(todoId);
    }
}

// 更新愿望卡片显示
function updateTodoCard(todoId) {
    const card = document.querySelector(`.todo-card[data-id="${todoId}"]`);
    if (!card) {
        // 通过父元素查找
        const allCards = document.querySelectorAll('.todo-card');
        allCards.forEach((c, index) => {
            if (index + 1 === todoId) {
                const checkbox = c.querySelector('input[type="checkbox"]');
                if (checkbox) {
                    const isCompleted = checkbox.checked;
                    if (isCompleted) {
                        c.classList.add('completed');
                    } else {
                        c.classList.remove('completed');
                    }
                }
            }
        });
    }
}

// 编辑愿望
function editTodo(todoId) {
    showToast(`编辑愿望 #${todoId}`);
    // 实际开发时显示编辑表单
}

// 筛选愿望
function filterTodos(filter) {
    const filterTabs = document.querySelectorAll('.filter-section .filter-tab');
    const todoCards = document.querySelectorAll('.todo-card');

    // 更新tab状态
    filterTabs.forEach(tab => {
        tab.classList.remove('active');
        if (tab.dataset.filter === filter) {
            tab.classList.add('active');
        }
    });

    // 筛选卡片
    todoCards.forEach(card => {
        const status = card.dataset.status;
        if (filter === 'all') {
            card.style.display = 'flex';
        } else if (filter === status) {
            card.style.display = 'flex';
        } else {
            card.style.display = 'none';
        }
    });
}

// 庆祝动画
function triggerCelebration() {
    // 创建庆祝特效
    const celebration = document.createElement('div');
    celebration.className = 'celebration-overlay';
    celebration.innerHTML = `
        <div class="celebration-content">
            <div class="celebration-emoji">🎉</div>
            <div class="celebration-text">太棒了！</div>
        </div>
    `;
    document.body.appendChild(celebration);

    setTimeout(() => {
        celebration.remove();
    }, 2000);
}

// ===== 导出新函数 =====
window.recordMyMood = recordMyMood;
window.viewPartnerMood = viewPartnerMood;
window.comfortPartner = comfortPartner;
window.showRecordMood = showRecordMood;
window.prevMoodMonth = prevMoodMonth;
window.nextMoodMonth = nextMoodMonth;
window.viewDayMood = viewDayMood;
window.toggleTodoStatus = toggleTodoStatus;
window.editTodo = editTodo;
window.filterTodos = filterTodos;

// ===== 模态框管理 =====
let currentEditingTodoId = null;
let selectedMood = null;
let currentPhotoIndex = 0;
let photosData = [];
let confirmCallback = null;
let currentTodoPriority = 'medium';

// 通用模态框显示/隐藏
function showModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.add('show');
    }
}

function hideModal(modalId) {
    const modal = document.getElementById(modalId);
    if (modal) {
        modal.classList.remove('show');
    }
}

// ===== 情绪记录模态框 =====
function showRecordMood() {
    showModal('moodModal');
    // 重置表单
    selectedMood = null;
    document.querySelectorAll('.mood-option').forEach(opt => opt.classList.remove('selected'));
    document.getElementById('moodLevel').value = 3;
    document.getElementById('moodReason').value = '';
    document.getElementById('reasonCharCount').textContent = '0';
    document.getElementById('submitMoodBtn').disabled = true;
}

function closeMoodModal() {
    hideModal('moodModal');
}

function selectMood(moodType) {
    selectedMood = moodType;

    // 更新选中状态
    document.querySelectorAll('.mood-option').forEach(opt => {
        opt.classList.remove('selected');
    });
    document.querySelector(`.mood-option[data-mood="${moodType}"]`).classList.add('selected');

    // 启用保存按钮
    document.getElementById('submitMoodBtn').disabled = false;
}

function updateMoodLevel(value) {
    // 可以在这里添加更多反馈，比如显示当前等级
    console.log('Mood level:', value);
}

// 监听textarea字符数
document.addEventListener('DOMContentLoaded', function() {
    const moodReason = document.getElementById('moodReason');
    if (moodReason) {
        moodReason.addEventListener('input', function() {
            document.getElementById('reasonCharCount').textContent = this.value.length;
        });
    }
});

function submitMoodRecord() {
    if (!selectedMood) {
        showToast('请选择心情');
        return;
    }

    const moodLevel = document.getElementById('moodLevel').value;
    const reason = document.getElementById('moodReason').value;

    // 这里应该调用API保存数据
    console.log('Submit mood:', {
        mood: selectedMood,
        level: moodLevel,
        reason: reason,
        date: new Date().toISOString().split('T')[0]
    });

    showToast('😊 心情记录成功！');
    closeMoodModal();

    // 模拟更新首页卡片（实际应该重新从API获取）
    updateHomeMoodCard();
}

function updateHomeMoodCard() {
    // 更新首页的情绪卡片显示
    const myMoodCard = document.querySelector('.mood-card.my-mood');
    if (myMoodCard) {
        // 实际开发中应该从API获取最新数据并更新UI
        showToast('首页心情已更新');
    }
}

// ===== 愿望清单模态框 =====
function showAddTodo() {
    currentEditingTodoId = null;
    document.getElementById('todoModalTitle').textContent = '添加新愿望';
    document.getElementById('todoTitle').value = '';
    document.getElementById('todoDescription').value = '';
    document.getElementById('todoDate').value = '';
    document.getElementById('todoTitleError').textContent = '';
    currentTodoPriority = 'medium';

    // 重置优先级选择
    document.querySelectorAll('.priority-option').forEach(opt => {
        opt.classList.remove('active');
    });
    document.querySelector('.priority-option[data-priority="medium"]').classList.add('active');

    showModal('todoModal');
}

function closeTodoModal() {
    hideModal('todoModal');
    currentEditingTodoId = null;
}

function selectPriority(priority) {
    currentTodoPriority = priority;
    document.querySelectorAll('.priority-option').forEach(opt => {
        opt.classList.remove('active');
    });
    document.querySelector(`.priority-option[data-priority="${priority}"]`).classList.add('active');
}

function editTodo(todoId) {
    currentEditingTodoId = todoId;

    // 从todosData中获取数据
    const todo = todosData.find(t => t.id === todoId);
    if (!todo) return;

    document.getElementById('todoModalTitle').textContent = '编辑愿望';
    document.getElementById('todoTitle').value = todo.title;
    document.getElementById('todoDescription').value = todo.description || '';
    document.getElementById('todoDate').value = todo.targetDate || '';

    // 设置优先级
    currentTodoPriority = todo.priority || 'medium';
    document.querySelectorAll('.priority-option').forEach(opt => {
        opt.classList.remove('active');
    });
    document.querySelector(`.priority-option[data-priority="${currentTodoPriority}"]`).classList.add('active');

    showModal('todoModal');
}

function submitTodoForm() {
    const title = document.getElementById('todoTitle').value.trim();
    const description = document.getElementById('todoDescription').value.trim();
    const targetDate = document.getElementById('todoDate').value;

    // 表单验证
    if (!title) {
        document.getElementById('todoTitleError').textContent = '请输入愿望标题';
        return;
    }

    const todoData = {
        title,
        description,
        targetDate,
        priority: currentTodoPriority,
        status: 'pending'
    };

    if (currentEditingTodoId) {
        // 编辑现有愿望
        const todo = todosData.find(t => t.id === currentEditingTodoId);
        if (todo) {
            Object.assign(todo, todoData);
        }
        showToast('✅ 愿望已更新');
    } else {
        // 添加新愿望
        todosData.push({
            id: todosData.length + 1,
            ...todoData
        });
        showToast('✨ 新愿望已添加');
    }

    closeTodoModal();

    // 重新渲染愿望列表（实际开发中应该从API获取）
    renderTodoList();
}

function renderTodoList() {
    // 实际开发中这里应该重新从API获取数据并渲染
    console.log('Render todo list with data:', todosData);
    showToast('愿望列表已更新');
}

// ===== 照片查看模态框 =====
function viewPhoto(photoIndex) {
    currentPhotoIndex = photoIndex;

    // 假设有照片数据数组
    if (!photosData || photosData.length === 0) {
        photosData = [
            { url: 'https://via.placeholder.com/800x600/FFB6C1/FFFFFF?text=Photo+1', description: '美好的一天', date: '2025-07-15' },
            { url: 'https://via.placeholder.com/800x600/FFB6C1/FFFFFF?text=Photo+2', description: '浪漫时刻', date: '2025-07-14' },
            { url: 'https://via.placeholder.com/800x600/FFB6C1/FFFFFF?text=Photo+3', description: '甜蜜回忆', date: '2025-07-13' }
        ];
    }

    updatePhotoView();
    showModal('photoViewModal');
}

function updatePhotoView() {
    const photo = photosData[currentPhotoIndex];
    if (photo) {
        document.getElementById('photoViewImage').src = photo.url;
        document.getElementById('photoDescription').textContent = photo.description;
        document.getElementById('photoDate').textContent = photo.date;
    }
}

function closePhotoModal() {
    hideModal('photoViewModal');
}

function prevPhoto() {
    if (currentPhotoIndex > 0) {
        currentPhotoIndex--;
        updatePhotoView();
    }
}

function nextPhoto() {
    if (currentPhotoIndex < photosData.length - 1) {
        currentPhotoIndex++;
        updatePhotoView();
    }
}

// ===== 删除确认对话框 =====
function showDeleteConfirm(message, callback) {
    document.getElementById('confirmMessage').textContent = message || '确定要删除这项内容吗？此操作无法撤销。';
    confirmCallback = callback;
    showModal('confirmModal');
}

function closeConfirmModal() {
    hideModal('confirmModal');
    confirmCallback = null;
}

function confirmDelete() {
    if (confirmCallback) {
        confirmCallback();
    }
    closeConfirmModal();
}

// 删除愿望示例
function deleteTodo(todoId) {
    showDeleteConfirm('确定要删除这个愿望吗？', () => {
        const index = todosData.findIndex(t => t.id === todoId);
        if (index > -1) {
            todosData.splice(index, 1);
            showToast('🗑️ 愿望已删除');
            renderTodoList();
        }
    });
}

// ===== 加载状态 =====
function showLoading() {
    document.getElementById('loadingOverlay').classList.add('show');
}

function hideLoading() {
    document.getElementById('loadingOverlay').classList.remove('show');
}

// 模拟API调用示例
function mockApiCall() {
    showLoading();
    setTimeout(() => {
        hideLoading();
        showToast('数据加载成功');
    }, 1500);
}

// ===== 动态渲染情绪日历 =====
function renderMoodCalendar(year, month) {
    // Mock数据：有情绪记录的日期
    const moodRecords = {
        '2025-07-15': 'HAPPY',
        '2025-07-14': 'LOVE',
        '2025-07-10': 'SAD',
        '2025-07-08': 'EXCITED',
        '2025-07-05': 'NEUTRAL'
    };

    const calendar = document.querySelector('.mood-calendar-grid');
    if (!calendar) return;

    // 计算当月信息
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const today = new Date();
    const isCurrentMonth = today.getFullYear() === year && today.getMonth() === month;

    // 清空日历
    calendar.innerHTML = '';

    // 添加星期标题
    const weekdays = ['日', '一', '二', '三', '四', '五', '六'];
    weekdays.forEach(day => {
        const weekdayEl = document.createElement('div');
        weekdayEl.className = 'calendar-weekday';
        weekdayEl.textContent = day;
        calendar.appendChild(weekdayEl);
    });

    // 添加空白格子（月初）
    for (let i = 0; i < firstDay; i++) {
        const emptyEl = document.createElement('div');
        emptyEl.className = 'calendar-day empty';
        calendar.appendChild(emptyEl);
    }

    // 添加日期
    for (let day = 1; day <= daysInMonth; day++) {
        const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        const dayEl = document.createElement('div');
        dayEl.className = 'calendar-day';

        // 检查是否是今天
        if (isCurrentMonth && day === today.getDate()) {
            dayEl.classList.add('today');
        }

        // 检查是否有情绪记录
        if (moodRecords[dateStr]) {
            dayEl.classList.add('has-mood');
            dayEl.dataset.mood = moodRecords[dateStr];

            // 添加情绪标记
            const moodIndicator = document.createElement('div');
            moodIndicator.className = 'mood-indicator';
            moodIndicator.style.background = getMoodColor(moodRecords[dateStr]);
            dayEl.appendChild(moodIndicator);
        }

        // 日期数字
        const dateNumber = document.createElement('span');
        dateNumber.textContent = day;
        dayEl.appendChild(dateNumber);

        // 点击事件
        dayEl.onclick = () => viewDayMood(dateStr);

        calendar.appendChild(dayEl);
    }
}

function getMoodColor(moodType) {
    const colors = {
        'HAPPY': '#FFD700',
        'LOVE': '#FF69B4',
        'SAD': '#87CEEB',
        'EXCITED': '#FF6347',
        'NEUTRAL': '#C0C0C0',
        'ANXIOUS': '#FFA500',
        'ANGRY': '#DC143C',
        'MISS': '#DDA0DD'
    };
    return colors[moodType] || '#C0C0C0';
}

// 更新月份时重新渲染日历
const originalPrevMoodMonth = window.prevMoodMonth;
const originalNextMoodMonth = window.nextMoodMonth;

window.prevMoodMonth = function() {
    originalPrevMoodMonth();
    renderMoodCalendar(currentMoodMonth.getFullYear(), currentMoodMonth.getMonth());
};

window.nextMoodMonth = function() {
    originalNextMoodMonth();
    renderMoodCalendar(currentMoodMonth.getFullYear(), currentMoodMonth.getMonth());
};

// 页面加载时渲染当前月份
document.addEventListener('DOMContentLoaded', function() {
    if (document.querySelector('.mood-calendar-grid')) {
        renderMoodCalendar(currentMoodMonth.getFullYear(), currentMoodMonth.getMonth());
    }
});

// ===== 导出新函数 =====
window.showRecordMood = showRecordMood;
window.closeMoodModal = closeMoodModal;
window.selectMood = selectMood;
window.updateMoodLevel = updateMoodLevel;
window.submitMoodRecord = submitMoodRecord;
window.showAddTodo = showAddTodo;
window.closeTodoModal = closeTodoModal;
window.selectPriority = selectPriority;
window.submitTodoForm = submitTodoForm;
window.deleteTodo = deleteTodo;
window.viewPhoto = viewPhoto;
window.closePhotoModal = closePhotoModal;
window.prevPhoto = prevPhoto;
window.nextPhoto = nextPhoto;
window.showDeleteConfirm = showDeleteConfirm;
window.closeConfirmModal = closeConfirmModal;
window.confirmDelete = confirmDelete;
window.showLoading = showLoading;
window.hideLoading = hideLoading;

// ===== 纪念日页面功能 =====
let currentCalendarDate = new Date();
let currentAnniversaryView = 'list';

// 纪念日数据（Mock）
const anniversariesData = [
    { id: 1, title: '小仙女的生日', date: '2025-07-28', type: 'birthday', important: true, notifyEnabled: true },
    { id: 2, title: '恋爱1000天', date: '2025-08-14', type: 'love', important: false, notifyEnabled: false },
    { id: 3, title: '恋爱纪念日', date: '2025-01-14', type: 'special', important: true, notifyEnabled: true },
    { id: 4, title: '情人节', date: '2025-02-14', type: 'valentine', important: true, notifyEnabled: true }
];

// 视图切换
function switchAnniversaryView(view) {
    currentAnniversaryView = view;

    // 更新按钮状态
    document.querySelectorAll('.view-toggle .toggle-btn').forEach(btn => {
        btn.classList.remove('active');
        if (btn.dataset.view === view) {
            btn.classList.add('active');
        }
    });

    // 切换容器显示
    const calendarView = document.querySelector('.calendar-view-container');
    const listView = document.querySelector('.list-view-container');

    if (view === 'calendar') {
        calendarView.style.display = 'block';
        listView.style.display = 'none';
        renderAnniversaryCalendar();
    } else {
        calendarView.style.display = 'none';
        listView.style.display = 'block';
    }
}

// 渲染纪念日日历
function renderAnniversaryCalendar() {
    const calendar = document.getElementById('calendarBody');
    if (!calendar) return;

    const year = currentCalendarDate.getFullYear();
    const month = currentCalendarDate.getMonth();

    // 更新标题
    const monthNames = ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月'];
    document.getElementById('calendarTitle').textContent = `${year}年${monthNames[month]}`;

    // 计算日历信息
    const firstDay = new Date(year, month, 1).getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const today = new Date();

    // 清空日历
    calendar.innerHTML = '';

    // 添加空白格子
    for (let i = 0; i < firstDay; i++) {
        const emptyEl = document.createElement('div');
        emptyEl.className = 'day empty';
        calendar.appendChild(emptyEl);
    }

    // 添加日期
    for (let day = 1; day <= daysInMonth; day++) {
        const dateStr = `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
        const dayEl = document.createElement('div');
        dayEl.className = 'day';

        // 检查是否是今天
        if (year === today.getFullYear() && month === today.getMonth() && day === today.getDate()) {
            dayEl.classList.add('today');
        }

        // 检查是否有纪念日
        const hasAnniversary = anniversariesData.some(ann => ann.date === dateStr);
        if (hasAnniversary) {
            dayEl.classList.add('has-anniversary');
            dayEl.onclick = () => showAnniversaryDetails(dateStr);
        }

        dayEl.textContent = day;
        calendar.appendChild(dayEl);
    }
}

// 上一个月
function prevMonth() {
    currentCalendarDate.setMonth(currentCalendarDate.getMonth() - 1);
    renderAnniversaryCalendar();
}

// 下一个月
function nextMonth() {
    currentCalendarDate.setMonth(currentCalendarDate.getMonth() + 1);
    renderAnniversaryCalendar();
}

// 显示纪念日详情
function showAnniversaryDetails(date) {
    const anniversaries = anniversariesData.filter(ann => ann.date === date);
    if (anniversaries.length > 0) {
        const titles = anniversaries.map(ann => ann.title).join('、');
        showToast(`📅 ${date}：${titles}`);
    }
}

// 纪念日筛选
function filterAnniversaries(filter) {
    // 更新tab状态
    document.querySelectorAll('.filter-tabs .filter-tab').forEach(tab => {
        tab.classList.remove('active');
        if (tab.dataset.filter === filter) {
            tab.classList.add('active');
        }
    });

    // 筛选卡片
    const cards = document.querySelectorAll('.anniversary-card');
    const today = new Date();
    const todayStr = today.toISOString().split('T')[0];

    cards.forEach(card => {
        const cardDate = card.dataset.date;
        let show = false;

        if (filter === 'all') {
            show = true;
        } else if (filter === 'upcoming') {
            show = cardDate >= todayStr;
        } else if (filter === 'passed') {
            show = cardDate < todayStr;
        }

        card.style.display = show ? 'flex' : 'none';
    });
}

// 添加纪念日
function showAddAnniversary() {
    showToast('💝 添加纪念日功能（待实现）');
}

// 编辑纪念日
function editAnniversary(id) {
    showToast(`✏️ 编辑纪念日 #${id}`);
}

// 切换通知
function toggleNotification(id) {
    showToast(`🔔 切换通知状态 #${id}`);
}

// 页面加载时初始化纪念日筛选
document.addEventListener('DOMContentLoaded', function() {
    // 为筛选标签添加点击事件
    document.querySelectorAll('.filter-tabs .filter-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            filterAnniversaries(this.dataset.filter);
        });
    });

    // 初始化纪念日日历
    if (document.getElementById('calendarBody')) {
        renderAnniversaryCalendar();
    }
});

// ===== 相册页面功能 =====
// 相册照片数据（Mock）
const albumPhotosData = [
    { id: 1, url: 'https://via.placeholder.com/300x300/FFB6C1/FFFFFF?text=1', description: '美好时光', date: '2025-07-15' },
    { id: 2, url: 'https://via.placeholder.com/300x300/FFB6C1/FFFFFF?text=2', description: '甜蜜瞬间', date: '2025-07-14' },
    { id: 3, url: 'https://via.placeholder.com/300x300/FFB6C1/FFFFFF?text=3', description: '快乐回忆', date: '2025-07-13' },
    { id: 4, url: 'https://via.placeholder.com/300x300/FFB6C1/FFFFFF?text=4', description: '浪漫日落', date: '2025-07-12' },
    { id: 5, url: 'https://via.placeholder.com/300x300/FFB6C1/FFFFFF?text=5', description: '幸福时刻', date: '2025-07-11' },
    { id: 6, url: 'https://via.placeholder.com/300x300/FFB6C1/FFFFFF?text=6', description: '温馨瞬间', date: '2025-07-10' }
];

// 切换选择模式
function toggleSelectMode() {
    isSelectionMode = !isSelectionMode;
    selectedPhotos.clear();

    const selectBtn = document.querySelector('.select-btn');
    const photoItems = document.querySelectorAll('.photo-item');

    if (isSelectionMode) {
        selectBtn.innerHTML = '<i class="fas fa-times"></i>';
        photoItems.forEach(item => item.classList.add('select-mode'));
        showToast('✅ 已进入选择模式');
    } else {
        selectBtn.innerHTML = '<i class="fas fa-check-square"></i>';
        photoItems.forEach(item => {
            item.classList.remove('select-mode', 'selected');
        });
        showToast('已退出选择模式');
    }
}

// 点击照片
function clickPhoto(photoId, event) {
    if (isSelectionMode) {
        // 选择模式：切换选中状态
        event.stopPropagation();
        const photoItem = event.currentTarget;

        if (selectedPhotos.has(photoId)) {
            selectedPhotos.delete(photoId);
            photoItem.classList.remove('selected');
        } else {
            selectedPhotos.add(photoId);
            photoItem.classList.add('selected');
        }

        showToast(`已选择 ${selectedPhotos.size} 张照片`);
    } else {
        // 查看模式：显示大图
        const photoIndex = albumPhotosData.findIndex(p => p.id === photoId);
        if (photoIndex !== -1) {
            photosData = albumPhotosData;
            viewPhoto(photoIndex);
        }
    }
}

// 添加照片
function showAddPhoto() {
    showToast('📷 添加照片功能（待实现）');
}

// 批量删除照片
function batchDeletePhotos() {
    if (selectedPhotos.size === 0) {
        showToast('请先选择要删除的照片');
        return;
    }

    showDeleteConfirm(`确定要删除选中的 ${selectedPhotos.size} 张照片吗？`, () => {
        showToast(`🗑️ 已删除 ${selectedPhotos.size} 张照片`);
        selectedPhotos.clear();
        toggleSelectMode();
    });
}

// 页面加载时为照片添加点击事件
document.addEventListener('DOMContentLoaded', function() {
    const photoItems = document.querySelectorAll('.photo-item');
    photoItems.forEach((item, index) => {
        const photoId = index + 1;
        item.onclick = (e) => clickPhoto(photoId, e);
    });
});

// ===== 导出纪念日和相册函数 =====
window.switchAnniversaryView = switchAnniversaryView;
window.prevMonth = prevMonth;
window.nextMonth = nextMonth;
window.filterAnniversaries = filterAnniversaries;
window.showAddAnniversary = showAddAnniversary;
window.editAnniversary = editAnniversary;
window.toggleNotification = toggleNotification;
window.toggleSelectMode = toggleSelectMode;
window.clickPhoto = clickPhoto;
window.showAddPhoto = showAddPhoto;
window.batchDeletePhotos = batchDeletePhotos;