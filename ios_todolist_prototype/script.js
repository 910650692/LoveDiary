// 全局变量
let todos = [
    {
        id: 1,
        title: "一起去旅行",
        description: "计划一次浪漫的周末旅行，去海边看日落",
        time: "2024-01-15 14:30",
        completed: false,
        completedTime: null
    },
    {
        id: 2,
        title: "看一场电影",
        description: "一起去看最新的爱情电影",
        time: "2024-01-10 19:00",
        completed: true,
        completedTime: "2024-01-12 20:30"
    },
    {
        id: 3,
        title: "学习做饭",
        description: "一起学习做一道新菜",
        time: "2024-01-08 16:45",
        completed: false,
        completedTime: null
    },
    {
        id: 4,
        title: "拍情侣写真",
        description: "",
        time: "2024-01-05 10:20",
        completed: false,
        completedTime: null
    }
];

// DOM 加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
});

// 初始化应用
function initializeApp() {
    setupEventListeners();
    setupSwipeToDelete();
    setupPullToRefresh();
}

// 设置事件监听器
function setupEventListeners() {
    // 模态框背景点击关闭
    document.getElementById('addModal').addEventListener('click', function(e) {
        if (e.target === this) {
            hideAddModal();
        }
    });

    // 表单输入验证
    document.getElementById('todoTitle').addEventListener('input', validateForm);
    document.getElementById('todoDescription').addEventListener('input', validateForm);
}

// 显示添加模态框
function showAddModal() {
    const modal = document.getElementById('addModal');
    modal.classList.add('show');
    document.getElementById('todoTitle').focus();
    
    // 添加震动反馈
    addShakeEffect();
}

// 隐藏添加模态框
function hideAddModal() {
    const modal = document.getElementById('addModal');
    modal.classList.remove('show');
    
    // 清空表单
    document.getElementById('todoTitle').value = '';
    document.getElementById('todoDescription').value = '';
}

// 验证表单
function validateForm() {
    const title = document.getElementById('todoTitle').value.trim();
    const saveBtn = document.querySelector('.save-btn');
    
    if (title.length > 0) {
        saveBtn.style.opacity = '1';
        saveBtn.style.pointerEvents = 'auto';
    } else {
        saveBtn.style.opacity = '0.5';
        saveBtn.style.pointerEvents = 'none';
    }
}

// 保存Todo
function saveTodo() {
    const title = document.getElementById('todoTitle').value.trim();
    const description = document.getElementById('todoDescription').value.trim();
    
    if (!title) {
        alert('请输入愿望标题');
        return;
    }
    
    const newTodo = {
        id: Date.now(),
        title: title,
        description: description,
        time: new Date().toLocaleString('zh-CN'),
        completed: false,
        completedTime: null
    };
    
    todos.unshift(newTodo);
    renderTodos();
    hideAddModal();
    
    // 添加震动反馈
    addShakeEffect();
    
    // 显示成功提示
    showSuccessMessage('愿望添加成功！');
}

// 切换完成状态
function toggleComplete(id) {
    const todo = todos.find(t => t.id === id);
    if (!todo) return;
    
    // 找到对应的按钮元素
    const buttonElement = event.target.closest('.complete-btn');
    
    todo.completed = !todo.completed;
    
    if (todo.completed) {
        todo.completedTime = new Date().toLocaleString('zh-CN');
        // 播放庆祝动画
        playCelebrationAnimation(id);
        // 添加背景闪烁效果
        addFlashEffect();
        
        // 添加按钮弹出效果
        addButtonPopEffect(buttonElement);
    } else {
        todo.completedTime = null;
    }
    
    renderTodos();
    
    // 添加优雅的反馈效果
    addFeedbackEffect();
}

// 渲染Todo列表
function renderTodos() {
    const todoList = document.getElementById('todoList');
    todoList.innerHTML = '';
    
    todos.forEach(todo => {
        const todoCard = createTodoCard(todo);
        todoList.appendChild(todoCard);
    });
}

// 创建Todo卡片
function createTodoCard(todo) {
    const card = document.createElement('div');
    card.className = `todo-card ${todo.completed ? 'completed' : ''}`;
    card.dataset.id = todo.id;
    
    card.innerHTML = `
        <div class="todo-content">
            <div class="todo-info">
                <h3 class="todo-title">${todo.title}</h3>
                ${todo.description ? `<p class="todo-description">${todo.description}</p>` : ''}
                <span class="todo-time">${todo.time}</span>
                ${todo.completedTime ? `<span class="completed-time">完成于 ${todo.completedTime}</span>` : ''}
            </div>
            <button class="complete-btn ${todo.completed ? 'completed' : ''}" onclick="toggleComplete(${todo.id})">
                <i class="fas ${todo.completed ? 'fa-check' : 'fa-circle'}"></i>
            </button>
        </div>
        <div class="delete-action">
            <i class="fas fa-trash"></i>
        </div>
    `;
    
    return card;
}

// 播放庆祝动画
function playCelebrationAnimation(todoId) {
    const card = document.querySelector(`[data-id="${todoId}"]`);
    const rect = card.getBoundingClientRect();
    const container = document.getElementById('celebrationContainer');
    
    // 创建上升的小星星
    for (let i = 0; i < 6; i++) {
        setTimeout(() => {
            const sparkle = document.createElement('div');
            sparkle.className = 'sparkle-rise';
            sparkle.innerHTML = '✨';
            sparkle.style.left = (rect.left + rect.width / 2 + (Math.random() - 0.5) * 80) + 'px';
            sparkle.style.top = (rect.top + rect.height / 2) + 'px';
            sparkle.style.fontSize = '16px';
            sparkle.style.color = '#FF6B9D';
            container.appendChild(sparkle);
            
            // 动画结束后移除元素
            setTimeout(() => {
                sparkle.remove();
            }, 1000);
        }, i * 80);
    }
    
    // 创建爱心动画
    for (let i = 0; i < 3; i++) {
        setTimeout(() => {
            const heart = document.createElement('div');
            heart.className = 'celebration-heart';
            heart.innerHTML = '💕';
            heart.style.left = (rect.left + rect.width / 2 + (Math.random() - 0.5) * 60) + 'px';
            heart.style.top = (rect.top + rect.height / 2) + 'px';
            container.appendChild(heart);
            
            // 动画结束后移除元素
            setTimeout(() => {
                heart.remove();
            }, 2000);
        }, i * 150);
    }
}

// 反馈效果配置
const feedbackConfig = {
    type: 'ripple', // 可选: 'ripple', 'slide', 'fade', 'color', 'button', 'none'
    effects: {
        ripple: 'ripple-effect',
        slide: 'slide-in',
        fade: 'fade-in-scale',
        color: 'color-transition',
        button: 'button-pop',
        none: ''
    }
};

// 添加优雅的反馈效果
function addFeedbackEffect() {
    const container = document.querySelector('.container');
    const effectClass = feedbackConfig.effects[feedbackConfig.type];
    
    if (effectClass) {
        container.classList.add(effectClass);
        setTimeout(() => {
            container.classList.remove(effectClass);
        }, 1000);
    }
}

// 添加按钮弹出效果
function addButtonPopEffect(buttonElement) {
    if (buttonElement) {
        buttonElement.classList.add('button-pop');
        setTimeout(() => {
            buttonElement.classList.remove('button-pop');
        }, 300);
    }
}

// 添加卡片滑入效果
function addCardSlideEffect(cardElement) {
    if (cardElement) {
        cardElement.classList.add('card-slide-up');
        setTimeout(() => {
            cardElement.classList.remove('card-slide-up');
        }, 500);
    }
}

// 添加震动反馈效果（保留原函数名以兼容）
function addShakeEffect() {
    addFeedbackEffect();
}

// 添加背景闪烁效果
function addFlashEffect() {
    const body = document.body;
    body.classList.add('flash');
    setTimeout(() => {
        body.classList.remove('flash');
    }, 500);
}

// 设置左滑删除功能
function setupSwipeToDelete() {
    let startX = 0;
    let currentX = 0;
    let isDragging = false;
    
    document.addEventListener('touchstart', function(e) {
        const card = e.target.closest('.todo-card');
        if (!card) return;
        
        startX = e.touches[0].clientX;
        currentX = startX;
        isDragging = true;
        
        card.style.transition = 'none';
    });
    
    document.addEventListener('touchmove', function(e) {
        if (!isDragging) return;
        
        currentX = e.touches[0].clientX;
        const diffX = currentX - startX;
        
        if (diffX < 0) {
            const card = e.target.closest('.todo-card');
            if (card) {
                card.style.transform = `translateX(${Math.max(diffX, -80)}px)`;
            }
        }
    });
    
    document.addEventListener('touchend', function(e) {
        if (!isDragging) return;
        
        const card = e.target.closest('.todo-card');
        if (!card) return;
        
        const diffX = currentX - startX;
        
        if (diffX < -40) {
            // 显示删除按钮
            card.classList.add('swiped');
        } else {
            // 恢复原位
            card.classList.remove('swiped');
        }
        
        card.style.transition = 'transform 0.3s ease';
        card.style.transform = '';
        isDragging = false;
    });
    
    // 点击删除按钮
    document.addEventListener('click', function(e) {
        if (e.target.closest('.delete-action')) {
            const card = e.target.closest('.todo-card');
            const todoId = parseInt(card.dataset.id);
            deleteTodo(todoId);
        }
    });
}

// 删除Todo
function deleteTodo(id) {
    if (confirm('确定要删除这个愿望吗？')) {
        todos = todos.filter(todo => todo.id !== id);
        renderTodos();
        addShakeEffect();
        showSuccessMessage('愿望已删除');
    }
}

// 设置下拉刷新
function setupPullToRefresh() {
    let startY = 0;
    let currentY = 0;
    let isPulling = false;
    
    document.addEventListener('touchstart', function(e) {
        if (window.scrollY === 0) {
            startY = e.touches[0].clientY;
            isPulling = true;
        }
    });
    
    document.addEventListener('touchmove', function(e) {
        if (!isPulling) return;
        
        currentY = e.touches[0].clientY;
        const diffY = currentY - startY;
        
        if (diffY > 50) {
            const pullRefresh = document.getElementById('pullRefresh');
            pullRefresh.classList.add('show');
        }
    });
    
    document.addEventListener('touchend', function(e) {
        if (!isPulling) return;
        
        const diffY = currentY - startY;
        
        if (diffY > 100) {
            // 触发刷新
            refreshTodos();
        }
        
        const pullRefresh = document.getElementById('pullRefresh');
        pullRefresh.classList.remove('show');
        isPulling = false;
    });
}

// 刷新Todo列表
function refreshTodos() {
    // 模拟刷新
    setTimeout(() => {
        showSuccessMessage('刷新成功！');
    }, 1000);
}

// 显示成功消息
function showSuccessMessage(message) {
    const toast = document.createElement('div');
    toast.style.cssText = `
        position: fixed;
        top: 20px;
        left: 50%;
        transform: translateX(-50%);
        background: linear-gradient(135deg, #FF6B9D, #FF8FAB);
        color: white;
        padding: 16px 28px;
        border-radius: 25px;
        font-size: 14px;
        font-weight: 500;
        z-index: 3000;
        animation: fadeIn 0.4s ease;
        box-shadow: 0 8px 25px rgba(255, 107, 157, 0.3);
        backdrop-filter: blur(10px);
    `;
    toast.innerHTML = `<i class="fas fa-check-circle" style="margin-right: 8px;"></i>${message}`;
    
    document.body.appendChild(toast);
    
    setTimeout(() => {
        toast.style.animation = 'fadeOut 0.4s ease';
        setTimeout(() => {
            toast.remove();
        }, 400);
    }, 2500);
}

// 添加淡出动画
const style = document.createElement('style');
style.textContent = `
    @keyframes fadeOut {
        from { opacity: 1; }
        to { opacity: 0; }
    }
`;
document.head.appendChild(style);

// 切换反馈效果类型
function changeFeedbackType() {
    const select = document.getElementById('feedbackType');
    feedbackConfig.type = select.value;
    
    // 显示提示
    showSuccessMessage(`已切换到${select.options[select.selectedIndex].text}效果`);
}

// 导出函数供HTML调用
window.showAddModal = showAddModal;
window.hideAddModal = hideAddModal;
window.saveTodo = saveTodo;
window.toggleComplete = toggleComplete;
window.changeFeedbackType = changeFeedbackType; 