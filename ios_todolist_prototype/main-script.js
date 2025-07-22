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