<script setup lang="ts">
defineOptions({
  name: 'EditDialog'
});

const visible = defineModel<boolean>('visible', { default: false });

interface Props {
  fileMd5: string;
  currentOrgTag: string | null;
  currentOrgTagName: string | null;
  currentIsPublic: boolean;
}

const props = defineProps<Props>();

interface Emits {
  (e: 'success'): void;
}

const emit = defineEmits<Emits>();

const authStore = useAuthStore();
const loading = ref(false);

const { formRef, validate, restoreValidation } = useNaiveForm();
const { defaultRequiredRule } = useFormRules();

const model = ref({
  orgTag: null as string | null,
  orgTagName: '' as string | null,
  isPublic: false
});

const rules = ref<FormRules>({
  orgTag: defaultRequiredRule,
  isPublic: defaultRequiredRule
});

// 监听 visible 和 props 变化，确保数据同步
watch([visible, () => props.currentOrgTag, () => props.currentOrgTagName, () => props.currentIsPublic], ([vis, tag, tagName, pub]) => {
  if (vis) {
    model.value.orgTag = tag;
    model.value.orgTagName = tagName;
    model.value.isPublic = pub;
    nextTick(() => {
      restoreValidation();
    });
  }
}, { immediate: true });

function close() {
  visible.value = false;
}

function onUpdate(option: unknown) {
  if (option) model.value.orgTagName = (option as Api.OrgTag.Item).name;
}

async function handleSubmit() {
  await validate();
  loading.value = true;
  try {
    const { error } = await request({
      url: `/documents/${props.fileMd5}`,
      method: 'PUT',
      data: {
        orgTag: model.value.orgTag,
        isPublic: model.value.isPublic
      }
    });
    if (!error) {
      window.$message?.success('文档信息更新成功');
      emit('success');
      close();
    }
  } finally {
    loading.value = false;
  }
}
</script>

<template>
  <NModal
    v-model:show="visible"
    preset="dialog"
    title="编辑文档信息"
    :show-icon="false"
    :mask-closable="false"
    class="w-500px!"
  >
    <NForm ref="formRef" :model="model" :rules="rules" label-placement="left" :label-width="100" mt-10>
      <NFormItem label="组织标签" path="orgTag">
        <OrgTagCascader v-model:value="model.orgTag" @change="onUpdate" />
      </NFormItem>
      <NFormItem label="是否公开" path="isPublic">
        <NRadioGroup v-model:value="model.isPublic" name="editPublicGroup">
          <NSpace :size="16">
            <NRadio :value="true">公开</NRadio>
            <NRadio :value="false">私有</NRadio>
          </NSpace>
        </NRadioGroup>
      </NFormItem>
    </NForm>
    <template #action>
      <NSpace :size="16">
        <NButton @click="close">取消</NButton>
        <NButton type="primary" :loading="loading" @click="handleSubmit">保存</NButton>
      </NSpace>
    </template>
  </NModal>
</template>

<style scoped></style>
